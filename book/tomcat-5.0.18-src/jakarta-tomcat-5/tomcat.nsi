
; Tomcat script for Nullsoft Installer
; $Id: tomcat.nsi,v 1.40 2003/12/27 17:32:23 remm Exp $

  ;Compression options
  CRCCheck on
  SetCompress force
  SetCompressor lzma
  SetDatablockOptimize on

  Name "Apache Tomcat"

  ;Product information
  VIAddVersionKey ProductName "Apache Tomcat"
  VIAddVersionKey CompanyName "Apache Software Foundation"
  VIAddVersionKey LegalCopyright "Copyright (c) 1999-2004 The Apache Software Foundation"
  VIAddVersionKey FileDescription "Apache Tomcat Installer"
  VIAddVersionKey FileVersion "2.0"
  VIAddVersionKey ProductVersion "@VERSION@"
  VIAddVersionKey Comments "jakarta.apache.org/tomcat"
  VIAddVersionKey InternalName "jakarta-tomcat-@VERSION@.exe"
  VIProductVersion @VERSION@.0

!include "MUI.nsh"

;--------------------------------
;Configuration

  !define MUI_FINISHPAGE_SHOWREADME "$INSTDIR\webapps\ROOT\RELEASE-NOTES.txt"
  !define MUI_FINISHPAGE_RUN $INSTDIR\bin\tomcatw.exe
  !define MUI_FINISHPAGE_RUN_PARAMETERS //GT//Tomcat5
  !define MUI_FINISHPAGE_NOREBOOTSUPPORT

  !define MUI_ABORTWARNING

  !define TEMP1 $R0
  !define TEMP2 $R1

  !define MUI_ICON tomcat.ico
  !define MUI_UNICON tomcat.ico

  ;General
  OutFile tomcat-installer.exe

  ;Install Options pages
  LangString TEXT_JVM_TITLE ${LANG_ENGLISH} "Java Virtual Machine"
  LangString TEXT_JVM_SUBTITLE ${LANG_ENGLISH} "Java Virtual Machine path selection."
  LangString TEXT_JVM_PAGETITLE ${LANG_ENGLISH} ": Java Virtual Machine path selection"

  LangString TEXT_CONF_TITLE ${LANG_ENGLISH} "Configuration"
  LangString TEXT_CONF_SUBTITLE ${LANG_ENGLISH} "Tomcat basic configuration."
  LangString TEXT_CONF_PAGETITLE ${LANG_ENGLISH} ": Configuration Options"

  ;Install Page order
  !insertmacro MUI_PAGE_WELCOME
  !insertmacro MUI_PAGE_LICENSE INSTALLLICENSE
  !insertmacro MUI_PAGE_COMPONENTS
  !insertmacro MUI_PAGE_DIRECTORY
  Page custom SetConfiguration Void "$(TEXT_CONF_PAGETITLE)"
  Page custom SetChooseJVM Void "$(TEXT_JVM_PAGETITLE)"
  !insertmacro MUI_PAGE_INSTFILES
  !insertmacro MUI_PAGE_FINISH

  ;Uninstall Page order
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES

  ;License dialog
  LicenseData INSTALLLICENSE

  ;Component-selection page
    ;Descriptions
    LangString DESC_SecTomcat ${LANG_ENGLISH} "Install the Tomcat Servlet container."
    LangString DESC_SecTomcatCore ${LANG_ENGLISH} "Install the Tomcat Servlet container core."
    LangString DESC_SecTomcatService ${LANG_ENGLISH} "Automatically start Tomcat when the computer is started. This requires Windows NT 4.0, Windows 2000 or Windows XP."
    LangString DESC_SecTomcatSource ${LANG_ENGLISH} "Install the Tomcat source code."
    LangString DESC_SecTomcatDocs ${LANG_ENGLISH} "Install the Tomcat documentation bundle. This include documentation on the servlet container and its configuration options, on the Jasper JSP page compiler, as well as on the native webserver connectors."
    LangString DESC_SecMenu ${LANG_ENGLISH} "Create a Start Menu program group for Tomcat."
    LangString DESC_SecExamples ${LANG_ENGLISH} "Installs some examples web applications."

  ;Language
  !insertmacro MUI_LANGUAGE English

  ;Folder-select dialog
  InstallDir "$PROGRAMFILES\Apache Software Foundation\Tomcat 5.0"

  ;Install types
  InstType Normal
  InstType Minimum
  InstType Full

  ; Main registry key
  InstallDirRegKey HKLM "SOFTWARE\Apache Software Foundation\Tomcat\5.0" ""

  !insertmacro MUI_RESERVEFILE_INSTALLOPTIONS
  ReserveFile "jvm.ini"
  ReserveFile "config.ini"

;--------------------------------
;Installer Sections

SubSection "Tomcat" SecTomcat

Section "Core" SecTomcatCore

  SectionIn 1 2 3

  Call checkJvm

  SetOutPath $INSTDIR
  File tomcat.ico
  File LICENSE
  File /r bin
  File /r common
  File /r conf
  File /nonfatal /r shared
  File /nonfatal /r logs
  File /r server
  File /nonfatal /r work
  File /nonfatal /r temp
  SetOutPath $INSTDIR\webapps
  File /r webapps\balancer
  File /r webapps\ROOT

  !insertmacro MUI_INSTALLOPTIONS_READ $2 "jvm.ini" "Field 2" "State"
  CopyFiles /SILENT "$2\lib\tools.jar" "$INSTDIR\common\lib" 4500
  ClearErrors

  Call configure

  ExecWait '"$INSTDIR\bin\tomcatw.exe" //IS//Tomcat5 --DisplayName "Apache Tomcat" --Description "Apache Tomcat @VERSION@ Server - http://jakarta.apache.org/tomcat/"  --Install "$INSTDIR\bin\tomcat.exe" --ImagePath "$INSTDIR\bin\bootstrap.jar" --StartupClass org.apache.catalina.startup.Bootstrap;main;start --ShutdownClass org.apache.catalina.startup.Bootstrap;main;stop --Java java --JavaOptions -Xrs --Startup manual'

SectionEnd

Section "Service" SecTomcatService

  SectionIn 3

  !insertmacro MUI_INSTALLOPTIONS_READ $2 "jvm.ini" "Field 2" "State"
  Push $2
  Call findJVMPath
  Pop $2

  ExecWait '"$INSTDIR\bin\tomcatw.exe" //US//Tomcat5 --Startup auto'

  ClearErrors

SectionEnd

Section "Source Code" SecTomcatSource

  SectionIn 3
  SetOutPath $INSTDIR
  File /r src

SectionEnd

Section "Documentation" SecTomcatDocs

  SectionIn 1 3
  SetOutPath $INSTDIR\webapps
  File /r webapps\tomcat-docs

SectionEnd

SubSectionEnd

Section "Start Menu Items" SecMenu

  SectionIn 1 2 3

  !insertmacro MUI_INSTALLOPTIONS_READ $2 "jvm.ini" "Field 2" "State"

  SetOutPath "$SMPROGRAMS\Apache Tomcat 5.0"

  CreateShortCut "$SMPROGRAMS\Apache Tomcat 5.0\Tomcat Home Page.lnk" \
                 "http://jakarta.apache.org/tomcat"

  CreateShortCut "$SMPROGRAMS\Apache Tomcat 5.0\Welcome.lnk" \
                 "http://127.0.0.1:$R0/"

  CreateShortCut "$SMPROGRAMS\Apache Tomcat 5.0\Tomcat Administration.lnk" \
                 "http://127.0.0.1:$R0/admin/"

  IfFileExists "$INSTDIR\webapps\webapps\tomcat-docs" 0 NoDocumentaion

  CreateShortCut "$SMPROGRAMS\Apache Tomcat 5.0\Tomcat Documentation.lnk" \
                 "$INSTDIR\webapps\tomcat-docs\index.html"

NoDocumentaion:

  CreateShortCut "$SMPROGRAMS\Apache Tomcat 5.0\Uninstall Tomcat 5.0.lnk" \
                 "$INSTDIR\Uninstall.exe"

  CreateShortCut "$SMPROGRAMS\Apache Tomcat 5.0\Tomcat 5.0 Program Directory.lnk" \
                 "$INSTDIR"

  CreateShortCut "$SMPROGRAMS\Apache Tomcat 5.0\Start Tomcat.lnk" \
                 "$INSTDIR\bin\tomcatw.exe" \
                 '//GT//Tomcat5' \
                 "$INSTDIR\bin\tomcatw.exe" 1 SW_SHOWNORMAL

  CreateShortCut "$SMPROGRAMS\Apache Tomcat 5.0\Configure Tomcat.lnk" \
                 "$INSTDIR\bin\tomcatw.exe" \
                 '//ES//Tomcat5' \
                 "$INSTDIR\bin\tomcatw.exe" 0 SW_SHOWNORMAL

SectionEnd

Section "Examples" SecExamples

  SectionIn 1 3

  SetOverwrite on
  SetOutPath $INSTDIR\webapps
  File /r webapps\jsp-examples
  File /r webapps\servlets-examples

SectionEnd

Section -post

  ExecWait '"$INSTDIR\bin\tomcatw.exe" //US//Tomcat5 --JavaOptions -Dcatalina.home="\"$INSTDIR\""#-Djava.endorsed.dirs="\"$INSTDIR\common\endorsed\""#-Xrs --StdOutputFile "$INSTDIR\logs\stdout.log" --StdErrorFile "$INSTDIR\logs\stderr.log" --WorkingPath "$INSTDIR"'

  WriteUninstaller "$INSTDIR\Uninstall.exe"

  WriteRegStr HKLM "SOFTWARE\Apache Software Foundation\Tomcat\5.0" "InstallPath" $INSTDIR
  WriteRegStr HKLM "SOFTWARE\Apache Software Foundation\Tomcat\5.0" "Version" @VERSION@
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Apache Tomcat 5.0" \
                   "DisplayName" "Apache Tomcat 5.0 (remove only)"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Apache Tomcat 5.0" \
                   "UninstallString" '"$INSTDIR\Uninstall.exe"'

SectionEnd

Function .onInit

  ;Extract Install Options INI Files
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT "config.ini"
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT "jvm.ini"

FunctionEnd

Function SetChooseJVM
  !insertmacro MUI_HEADER_TEXT "$(TEXT_JVM_TITLE)" "$(TEXT_JVM_SUBTITLE)"
  Call findJavaPath
  Pop $3
  !insertmacro MUI_INSTALLOPTIONS_WRITE "jvm.ini" "Field 2" "State" $3
  !insertmacro MUI_INSTALLOPTIONS_DISPLAY "jvm.ini"
FunctionEnd

Function SetConfiguration
  !insertmacro MUI_HEADER_TEXT "$(TEXT_CONF_TITLE)" "$(TEXT_CONF_SUBTITLE)"
  !insertmacro MUI_INSTALLOPTIONS_DISPLAY "config.ini"
FunctionEnd

Function Void
FunctionEnd

;--------------------------------
;Descriptions

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecTomcat} $(DESC_SecTomcat)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecTomcatCore} $(DESC_SecTomcatCore)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecTomcatService} $(DESC_SecTomcatService)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecTomcatSource} $(DESC_SecTomcatSource)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecTomcatDocs} $(DESC_SecTomcatDocs)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecMenu} $(DESC_SecMenu)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecExamples} $(DESC_SecExamples)
!insertmacro MUI_FUNCTION_DESCRIPTION_END


; =====================
; FindJavaPath Function
; =====================
;
; Find the JAVA_HOME used on the system, and put the result on the top of the
; stack
; Will return an empty string if the path cannot be determined
;
Function findJavaPath

  ClearErrors

  ReadEnvStr $1 JAVA_HOME

  IfErrors 0 FoundJDK

  ClearErrors

  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$2" "JavaHome"
  ReadRegStr $3 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$2" "RuntimeLib"

  FoundJDK:

  IfErrors 0 NoErrors
  StrCpy $1 ""

NoErrors:

  ClearErrors

  ; Put the result in the stack
  Push $1

FunctionEnd


; ====================
; FindJVMPath Function
; ====================
;
; Find the full JVM path, and put the result on top of the stack
; Argument: JVM base path (result of findJavaPath)
; Will return an empty string if the path cannot be determined
;
Function findJVMPath

  Pop $1

  IfFileExists "$1\jre\bin\hotspot\jvm.dll" 0 TryJDK14
    StrCpy $2 "$1\jre\bin\hotspot\jvm.dll"
    Goto EndIfFileExists
  TryJDK14:
  IfFileExists "$1\jre\bin\server\jvm.dll" 0 TryClassic
    StrCpy $2 "$1\jre\bin\server\jvm.dll"
    Goto EndIfFileExists
  TryClassic:
  IfFileExists "$1\jre\bin\classic\jvm.dll" 0 JDKNotFound
    StrCpy $2 "$1\jre\bin\classic\jvm.dll"
    Goto EndIfFileExists
  JDKNotFound:
    SetErrors
  EndIfFileExists:

  IfErrors 0 FoundJVMPath

  ClearErrors

  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$1" "RuntimeLib"
  
  FoundJVMPath:

  IfErrors 0 NoErrors
  StrCpy $2 ""

NoErrors:

  ClearErrors

  ; Put the result in the stack
  Push $2

FunctionEnd


; ====================
; CheckJvm Function
; ====================
;
Function checkJvm

  !insertmacro MUI_INSTALLOPTIONS_READ $3 "jvm.ini" "Field 2" "State"
  IfFileExists "$3\bin\java.exe" NoErrors1
  MessageBox MB_OK "No Java Virtual Machine found."
  Quit
NoErrors1:
  Push $3
  Call findJVMPath
  Pop $4
  StrCmp $4 "" 0 NoErrors2
  MessageBox MB_OK "No Java Virtual Machine found."
  Quit
NoErrors2:

FunctionEnd

; ==================
; Configure Function
; ==================
;
; Display the configuration dialog boxes, read the values entered by the user,
; and build the configuration files
;
Function configure

  !insertmacro MUI_INSTALLOPTIONS_READ $R0 "config.ini" "Field 2" "State"
  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "config.ini" "Field 5" "State"
  !insertmacro MUI_INSTALLOPTIONS_READ $R2 "config.ini" "Field 7" "State"

  StrCpy $R4 'port="$R0"'
  StrCpy $R5 '<user name="$R1" password="$R2" roles="admin,manager" />'

  DetailPrint 'HTTP/1.1 Connector configured on port "$R0"'
  DetailPrint 'Admin user added: "$R1"'

  SetOutPath $TEMP
  File /r confinstall

  ; Build final server.xml
  Delete "$INSTDIR\conf\server.xml"
  FileOpen $R9 "$INSTDIR\conf\server.xml" w

  Push "$TEMP\confinstall\server_1.xml"
  Call copyFile
  FileWrite $R9 $R4
  Push "$TEMP\confinstall\server_2.xml"
  Call copyFile

  FileClose $R9

  DetailPrint "server.xml written"

  ; Build final tomcat-users.xml
  Delete "$INSTDIR\conf\tomcat-users.xml"
  FileOpen $R9 "$INSTDIR\conf\tomcat-users.xml" w

  Push "$TEMP\confinstall\tomcat-users_1.xml"
  Call copyFile
  FileWrite $R9 $R5
  Push "$TEMP\confinstall\tomcat-users_2.xml"
  Call copyFile

  FileClose $R9

  DetailPrint "tomcat-users.xml written"

  RMDir /r "$TEMP\confinstall"

FunctionEnd


; =================
; CopyFile Function
; =================
;
; Copy specified file contents to $R9
;
Function copyFile

  ClearErrors

  Pop $0

  FileOpen $1 $0 r

 NoError:

  FileRead $1 $2
  IfErrors EOF 0
  FileWrite $R9 $2

  IfErrors 0 NoError

 EOF:

  FileClose $1

  ClearErrors

FunctionEnd


;--------------------------------
;Uninstaller Section

Section Uninstall

  Delete "$INSTDIR\modern.exe"
  Delete "$INSTDIR\Uninstall.exe"

  ; Delete Tomcat service
  ExecWait '"$INSTDIR\bin\tomcatw.exe" //DS//Tomcat5'
  ClearErrors

  DeleteRegKey HKCR "JSPFile"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Apache Tomcat 5.0"
  DeleteRegKey HKLM "SOFTWARE\Apache Software Foundation\Tomcat\5.0"
  RMDir /r "$SMPROGRAMS\Apache Tomcat 5.0"
  Delete "$INSTDIR\tomcat.ico"
  Delete "$INSTDIR\LICENSE"
  RMDir /r "$INSTDIR\bin"
  RMDir /r "$INSTDIR\common"
  Delete "$INSTDIR\conf\*.dtd"
  RMDir /r "$INSTDIR\shared"
  RMDir "$INSTDIR\logs"
  RMDir /r "$INSTDIR\server"
  RMDir /r "$INSTDIR\webapps\balancer"
  RMDir /r "$INSTDIR\webapps\ROOT"
  RMDir /r "$INSTDIR\webapps\tomcat-docs"
  RMDir /r "$INSTDIR\webapps\servlets-examples"
  RMDir /r "$INSTDIR\webapps\jsp-examples"
  RMDir "$INSTDIR\webapps"
  RMDir /r "$INSTDIR\work"
  RMDir /r "$INSTDIR\temp"
  RMDir /r "$INSTDIR\src"
  RMDir "$INSTDIR"

  ; if $INSTDIR was removed, skip these next ones
  IfFileExists "$INSTDIR" 0 Removed 
    MessageBox MB_YESNO|MB_ICONQUESTION \
      "Remove all files in your Tomcat 5.0 directory? (If you have anything\
 you created that you want to keep, click No)" IDNO Removed
    Delete "$INSTDIR\*.*" ; this would be skipped if the user hits no
    RMDir /r "$INSTDIR"
    Sleep 500
    IfFileExists "$INSTDIR" 0 Removed 
      MessageBox MB_OK|MB_ICONEXCLAMATION \
                 "Note: $INSTDIR could not be removed."
  Removed:

SectionEnd

;eof
