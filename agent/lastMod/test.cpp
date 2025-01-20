#include <iostream>
#include <sstream>
#include <iomanip>
#include <curl/curl.h>
#include <algorithm>
#include <fstream>

using namespace std;

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
void sendDataToJavaServlet(const string& timeCreated, const string& objectName, const string& accountDomain, const string& oldValue, const string& newValue, const string& message, const string& changedOn, const string& organization) {
    CURL* curl;
    CURLcode res;
    curl = curl_easy_init();

    if (curl) {
        string servletUrl = "http://localhost:8080/backend_war_exploded/StoreLog";
        string jsonData = "{\"TimeCreated\":\"" + timeCreated +
                             "\",\"objectName\":\"" + objectName +
                             "\",\"AccountDomain\":\"" + accountDomain +
                             "\",\"OldValue\":\"" + oldValue +
                             "\",\"NewValue\":\"" + newValue +
                             "\",\"Message\":\"" + message +
                             "\",\"ChangedOn\":\"" + changedOn +
                             "\",\"Organization\":\"" + organization + "\"}";

        struct curl_slist* headers = nullptr;
        headers = curl_slist_append(headers, "Content-Type: application/json");
        // curl_easy_setopt(curl, CURLOPT_URL, servletUrl.c_str());
        // curl_easy_setopt(curl, CURLOPT_POST, 1L);
        // curl_easy_setopt(curl, CURLOPT_POSTFIELDS, jsonData.c_str());
        // curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

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
    string remoteHost = "192.168.219.71";
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
                if ($message -match 'LDAP Display Name:\s(description|mail|givenName)') {
                    $changedOn = $matches[1]
                    if ($message -match 'Value:\s*(.*?)\r') {
                        $attributeValue = $matches[1]
                        $dn = $null
                        $objectName = $null
                        $accountDomain = $null
                        $class = $null
                        if ($message -match 'DN:\s+(.*)\r') {
                            $dn = $matches[1]
                        }
                        if ($message -match 'objectName:\s+(.*)\r') {
                            $objectName = $matches[1]
                        }
                        if ($message -match 'Account Domain:\s+(.*)\r') {
                            $accountDomain = $matches[1]
                        }
                        if ($dn -match 'CN=(.*?),') {
                            $objectName = $matches[1]
                        }
                        if ($message -match 'Class:\s+(.*)\r') {
                            $class = $matches[1]
                            $organization = $class
                        }
                        $oldValue = ''
                        $newValue = ''
                        if($message -match 'Operation:\s*Type:\s*%%14674'){
                            $newValue = $attributeValue
                        } 
                        elseif($message -match 'Operation:\s*Type:\s*%%14675'){
                            $oldValue = $attributeValue
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
    if(!data){
        cerr << "Failed to run command" << endl;
        return 1;
    }
    string objectName, accountDomain, oldValue, newValue, modifiedTime, message, changedOn, organization;
    bool validEntry = false;
    char buff[128];
    while(fgets(buff, sizeof(buff), data) != nullptr){
        string line(buff);
        if (line.find("User:") != string::npos) {
            validEntry = false;
        }
        if(line.find("objectName") != string::npos){
            objectName = line.substr(line.find(":") + 1);
            objectName = trim(objectName);
            validEntry = true;
        } 
        else if(line.find("Account Domain") != string::npos){
            accountDomain = line.substr(line.find(":") + 1);
            accountDomain = trim(accountDomain);
        }
        else if (line.find("Old Value") != string::npos) {
            oldValue = line.substr(line.find(":") + 1);
            oldValue = trim(oldValue);
        }
        else if (line.find("New Value") != string::npos) {
            newValue = line.substr(line.find(":") + 1);
            newValue = trim(newValue);
        }
        else if (line.find("Modified Time") != string::npos) {
            modifiedTime = line.substr(line.find(":") + 1);
            modifiedTime = trim(modifiedTime);
            modifiedTime = convertDateFormat(modifiedTime);
        }
        else if (line.find("Changed On") != string::npos) {
            changedOn = line.substr(line.find(":") + 1);
            changedOn = trim(changedOn);
        }  
        else if (line.find("Organization") != string::npos) {
            organization = line.substr(line.find(":") + 1);
            organization = trim(organization);
        }
        else if (line.find("Message") != string::npos) {
            message = line.substr(line.find(":") + 1);
            message = trim(message);
            if (validEntry) {
                cout << "objectName: " << objectName << endl;
                cout << "Account Domain: " << accountDomain << endl;
                cout << "Old Value: " << oldValue << endl;
                cout << "New Value: " << newValue << endl;
                cout << "Modified Time: " << modifiedTime << endl;
                cout << "Message: " << message << endl;
                cout << "Changed On: " << changedOn << endl;
                cout << "Organization: " << organization << endl;
                if (!objectName.empty() && !accountDomain.empty() && !oldValue.empty() && !newValue.empty() && !modifiedTime.empty() && !message.empty() && !changedOn.empty() && !organization.empty()) {
                    sendDataToJavaServlet(modifiedTime, objectName, accountDomain, oldValue, newValue, message, changedOn, organization);
                } else {
                    cerr << "Required fields are missing. Data not sent to Java Servlet." << endl;
                }
                
                validEntry = false;
            }
        }
    }
    pclose(data);
    string cleanupCommand = "sshpass -p '" + password + "' ssh " + username + "@" + remoteHost + " powershell.exe -Command \\\"Remove-Item -Path 'C:\\temp\\script.ps1' -Force\\\"";
    system(cleanupCommand.c_str());

    return 0;
}