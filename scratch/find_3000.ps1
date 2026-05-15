$processes = Get-CimInstance Win32_Process
$found = $processes | Where-Object { $_.CommandLine -like '*3000*' -or $_.Name -like '*3000*' }
$found | Select-Object Name, ProcessId, CommandLine | Format-Table -AutoSize
