# utils_bot
A telegram bot I made as a university project. Running instance of the bot: [@utiIs_bot](https://web.telegram.org/k/#@utiIs_bot)

## Functionality:
 - DadBot - if enabled replys with "Hi \<ur thext\>, I'm dad" truly fascinating feature
 - VmToText - if enabled Transcripts all new VMs in this chat, a bit slow because of the deployment environment I use
 - translate - translates all new messages into the selected language
 - everyone - pings all users that have enabled notifications using /notifications or the inline keyboard, list of users with enabled notifications can be viewed in /info -> "Notifications and /everyone"
 - ocr - replies withe the recognised text from an image captioned with /ocr. If no language is specified will default to English.
 - notifications - can schedule notifications for private and group chats. In the case of group chats, all users that joined the notifications group will be mentioned in the notification message.

## Libraries and dependencies:
  - Spring Boot
  - telegrambots-spring-boot-starter
  - liquibase
  - infinispan-cache
  - postgresql

## External APIs
 - [abstractapi](https://app.abstractapi.com/) for timezone identification
 - [ocr.space](https://ocr.space/ocrapi) for ocr
 - [libre translate](https://translate.fedilab.app/) for translation ([github patge](https://github.com/LibreTranslate/LibreTranslate))
 - [This](https://hub.docker.com/r/onerahmet/openai-whisper-asr-webservice#!) amaizing docker container with a [whisper](https://github.com/openai/whisper) model api

## Run with docker
Update all ur api keys in [docker-compose](https://github.com/RaduDobrovolschi/utils_bot/tree/main/src/main/docker), then run:
```
docker-compose up -d
```
