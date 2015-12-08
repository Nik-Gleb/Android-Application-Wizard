git clone %1 %2
cd .\%2
set "tempDir=%cd%"
git checkout -b dev
cd /d %3
xcopy %tempDir%\.git .git /s /e /h /i
xcopy %tempDir%\LICENSE /s /e /h
xcopy %tempDir%\README.md /s /e /h
rmdir /s /q %tempDir%
exit 0