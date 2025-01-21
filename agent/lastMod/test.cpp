#include <iostream>
#include <sstream>
#include <iomanip>
#include <curl/curl.h>
#include <algorithm>
#include <fstream>
#include <map>

using namespace std;

struct Entry {
    string objectName;
    string accountDomain;
    string oldValue;
    string newValue;
    string modifiedTime;
    string message;
    string changedOn;
    string organization;
};

string trim(const string& str) {
    size_t first = str.find_first_not_of(" \t\n\r+");
    size_t last = str.find_last_not_of(" \t\n\r+");
    return (first == string::npos || last == string::npos) ? "" : str.substr(first, last - first + 1);
}

string convertDateFormat(const string& input) {
    tm t = {};
    istringstream ss(trim(input));
    ss >> get_time(&t, "%m/%d/%Y %I:%M:%S %p");
    if (ss.fail()) {
        cerr << "Failed to parse date: " << input << endl;
        return "";
    }
    ostringstream oss;
    oss << put_time(&t, "%Y-%m-%d %H:%M:%S");
    return oss.str();
}

void sendDataToJavaServlet(const Entry& entry) {
    CURL* curl;
    CURLcode res;
    curl = curl_easy_init();
    if (curl) {
        string servletUrl = "http://localhost:8080/backend_war_exploded/StoreLog";
        string jsonData = "{\"TimeCreated\":\"" + entry.modifiedTime +
                             "\",\"objectName\":\"" + entry.objectName +
                             "\",\"AccountDomain\":\"" + entry.accountDomain +
                             "\",\"OldValue\":\"" + entry.oldValue +
                             "\",\"NewValue\":\"" + entry.newValue +
                             "\",\"Message\":\"" + entry.message +
                             "\",\"ChangedOn\":\"" + entry.changedOn +
                             "\",\"Organization\":\"" + entry.organization + "\"}";

        struct curl_slist* headers = nullptr;
        headers = curl_slist_append(headers, "Content-Type: application/json");
        curl_easy_setopt(curl, CURLOPT_URL, servletUrl.c_str());
        curl_easy_setopt(curl, CURLOPT_POST, 1L);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, jsonData.c_str());
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

        res = curl_easy_perform(curl);
        if (res != CURLE_OK) {
            cerr << "Failed to send data to Java Servlet: " << curl_easy_strerror(res) << endl;
        } else {
            cout << "Data sent to Java Servlet successfully" << endl;
        }
        curl_slist_free_all(headers);
        curl_easy_cleanup(curl);
    }
}

int main() {
    string remoteHost = "192.168.168.72";
    string username = "Administrator";
    string password = "Ram@123";

    string psScript = R"(
        $users = Get-ADUser -Filter * -Properties description, mail, givenName
        foreach ($user in $users) {
            $name = $user.SamobjectName
            $mail = $user.mail
            $firstName = $user.givenName
            Write-Host 'User: ' $name
            Write-Host 'Mail: ' $mail
            Write-Host 'First Name: ' $firstName
            Write-Host 'Fetching event log entries for user: ' $name
            Get-EventLog -LogName Security -InstanceId 5136 | ForEach-Object {
                $event = $_
                $message = $event.Message
                $modifiedTime = $event.TimeGenerated
                $changedOn = $null
                $organization = $null
                Write-Host 'Processing event: ' $message
                if($message -match 'LDAP Display Name:\s(description|mail|givenName)') {
                    $changedOn = $matches[1]
                    if($message -match 'Value:\s*(.*?)\r') {
                        $attributeValue = $matches[1]
                        $dn = $null
                        $objectName = $null
                        $accountDomain = $null
                        $class = $null
                        if($message -match 'DN:\s+(.*)\r') {
                            $dn = $matches[1]
                        }
                        if($message -match 'objectName:\s+(.*)\r') {
                            $objectName = $matches[1]
                        }
                        if($message -match 'Account Domain:\s+(.*)\r') {
                            $accountDomain = $matches[1]
                        }
                        if($dn -match 'CN=(.*?),') {
                            $objectName = $matches[1]
                        }
                        if($message -match 'Class:\s+(.*)\r') {
                            $class = $matches[1]
                            $organization = $class
                        }
                        $oldValue = ''
                        $newValue = ''
                        if($message -match 'Operation:\s*Type:\s*%%14674'){
                            $newValue = $attributeValue
                        } 
                        elseif($message -match 'Operation:\s*Type:\s*%%14675'){
                            $oldValue = $attributeValue;
                        }
                        [PSCustomObject]@{
                            'objectName'  = $objectName
                            'Account Domain' = $accountDomain
                            'Old Value'      = $oldValue
                            'New Value'      = $newValue
                            'Modified Time'  = $modifiedTime
                            'Message'        = $message
                            'Changed On'     = $changedOn
                            'Organization'   = $organization
                        }
                    }
                }
            }
        }
    )";

    ofstream scriptFile("script.ps1");
    scriptFile << psScript;
    scriptFile.close();

    string createDirCommand = "sshpass -p '" + password + "' ssh " + username + "@" + remoteHost + " \"powershell.exe -Command \\\"if (-Not (Test-Path -Path 'C:\\temp')) { New-Item -ItemType Directory -Path 'C:\\temp' }\\\"\"";
    system(createDirCommand.c_str());
    string copyCommand = "sshpass -p '" + password + "' scp script.ps1 " + username + "@" + remoteHost + ":C:/temp/script.ps1";
    system(copyCommand.c_str());
    string executeCommand = "sshpass -p '" + password + "' ssh " + username + "@" + remoteHost + " powershell.exe -File C:/temp/script.ps1";

    FILE* data = popen(executeCommand.c_str(), "r");
    if (!data) {
        cerr << "Failed to run command" << endl;
        return 1;
    }
    map<string, Entry> entries;
    char buff[128];
    string objectName, accountDomain, oldValue, newValue, modifiedTime, message, changedOn, organization;
    bool validEntry = false;

    while (fgets(buff, sizeof(buff), data) != nullptr) {
        string line(buff);
        if (line.find("User:") != string::npos) {
            validEntry = false;
        }
        if (line.find("objectName") != string::npos) {
            objectName = trim(line.substr(line.find(":") + 1));
            validEntry = true;
        } else if (line.find("Account Domain") != string::npos) {
            accountDomain = trim(line.substr(line.find(":") + 1));
        } else if (line.find("Old Value") != string::npos) {
            oldValue = trim(line.substr(line.find(":") + 1));
        } else if (line.find("New Value") != string::npos) {
            newValue = trim(line.substr(line.find(":") + 1));
        } else if (line.find("Modified Time") != string::npos) {
            modifiedTime = convertDateFormat(trim(line.substr(line.find(":") + 1)));
        } else if (line.find("Changed On") != string::npos) {
            changedOn = trim(line.substr(line.find(":") + 1));
        } else if (line.find("Organization") != string::npos) {
            organization = trim(line.substr(line.find(":") + 1));
        } else if (line.find("Message") != string::npos) {
            message = trim(line.substr(line.find(":") + 1));
            if (validEntry) {
                if (entries.find(modifiedTime) != entries.end()) {
                    if (entries[modifiedTime].oldValue.empty()) {
                        entries[modifiedTime].oldValue = oldValue;
                    }
                    if (entries[modifiedTime].newValue.empty()) {
                        entries[modifiedTime].newValue = newValue;
                    }
                } else {
                    entries[modifiedTime] = {objectName, accountDomain, oldValue, newValue, modifiedTime, message, changedOn, organization};
                }
                validEntry = false;
            }
        }
    }
    pclose(data);
    for (const auto& pair : entries) {
        const Entry& entry = pair.second;
        cout << "objectName: " << entry.objectName << endl;
        cout << "Account Domain: " << entry.accountDomain << endl;
        cout << "Old Value: " << entry.oldValue << endl;
        cout << "New Value: " << entry.newValue << endl;
        cout << "Modified Time: " << entry.modifiedTime << endl;
        cout << "Message: " << entry.message << endl;
        cout << "Changed On: " << entry.changedOn << endl;
        cout << "Organization: " << entry.organization << endl;

        if (!entry.objectName.empty() && !entry.accountDomain.empty() && !entry.modifiedTime.empty() && !entry.message.empty() && !entry.changedOn.empty() && !entry.organization.empty()) {
            sendDataToJavaServlet(entry);
        } else {
            cerr << "Required fields are missing. Data not sent to Java Servlet." << endl;
        }
    }

    string cleanupCommand = "sshpass -p '" + password + "' ssh " + username + "@" + remoteHost + " powershell.exe -Command \\\"Remove-Item -Path 'C:\\temp\\script.ps1' -Force\\\"";
    system(cleanupCommand.c_str());

    return 0;
}