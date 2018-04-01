@echo off

echo start time: %TIME% > time.tmp

del /F /Q logs\*.zip > nul

if "%1" == ""  (set PATTERN=**/JPPFSuite.java) else (set PATTERN=%1)
for /L %%i in (1,1,%2) do (
  echo(
  echo ***** Test run %%i *****
  del /F /Q *.log > nul
  call ant test.pattern2 -Dpattern=%PATTERN%
  if exist failure.log goto error
)

goto end

:error
del /F /Q failure.log

:end

echo end time:   %TIME% >> time.tmp
echo(
type time.tmp
del /F /Q time.tmp