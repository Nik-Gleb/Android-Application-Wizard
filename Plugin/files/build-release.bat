cd /d %~dp0
call ant release
echo f | xcopy /f /y ${projectName}-bin\${projectName}-release.apk build\${projectName}.apk
rmdir ${projectName}-bin\ /s /q
call ant -buildfile javadoc.xml
PAUSE