#!/bin/bash
#IP='44.237.219.139' # Staging
IP='54.212.15.213' # Prod
#scp -i ~/.ssh/id_ed25519 /Users/john.verwolf/code/rhododendra/build/libs/rhododendra-0.0.1-SNAPSHOT.jar ec2-user@"${IP}":/home/ec2-user/
#scp -i ~/.ssh/id_ed25519 -r /Users/john.verwolf/code/rhododendra/index ec2-user@"${IP}":/home/ec2-user/
scp -i ~/.ssh/id_ed25519 -r /Users/john.verwolf/code/rhododendra/start.sh ec2-user@"${IP}":/home/ec2-user/
scp -i ~/.ssh/id_ed25519 -r /Users/john.verwolf/code/rhododendra/stop.sh ec2-user@"${IP}":/home/ec2-user/

