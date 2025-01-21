
        $users = Get-ADUser -Filter * -Properties description, mail, givenName
        foreach ($user in $users) {
            $name = $user.SamAccountName
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
                if ($message -match 'LDAP Display Name:\s(description|mail|givenName|'')') {
                    $changedOn = $matches[1]
                    Write-Host "Changed On: $changedOn"
                    if ($message -match 'Value:\s*(.*?)\r') {
                        $attributeValue = $matches[1]
                        $dn = $null
                        $objectName = $null
                        $accountDomain = $null
                        $class = $null
                        if ($message -match 'DN:\s+(.*)\r') {
                            $dn = $matches[1]
                        }
                        if ($message -match 'Object Name:\s+(.*)\r') {
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
                            Write-Host "Organization: $organization"
                        }
                        $oldValue = ''
                        $newValue = ''
                        if ($message -match 'Operation:\s*Type:\s*%%14674'){
                            $newValue = $attributeValue
                        } 
                        elseif ($message -match 'Operation:\s*Type:\s*%%14675'){
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
    