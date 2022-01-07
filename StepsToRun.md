<!-- Steps to run project -->

# from main directory (this will run 4 terminals automatically)
./gradlew clean deployNodes
cd build/nodes
./runnodes
cd ../..

# open 3 terminals and run following command on each terminal
./gradlew runFarmerServer
./gradlew runSupplierServer
./gradlew runConsumerOrgServer

# after all 7 erminals are running, access the endpoints at:
http://localhost:10050      <!-- Farmer client -->
http://localhost:10060      <!-- Supplier client -->
http://localhost:10070      <!-- ConsumerOrganization client -->

# api's are - 
http://localhost:{port}/register?batchId={}&numTomato={}&toUser={}&senderDid={}&receiverDid={}
<sample>
http://localhost:10050/register?batchId=JAJAJA&numTomato=89&toUser=O=Supplier,L=Boston,C=US&senderDid=ABCD123&receiverDid=EFGH456
r

http://localhost:{port}/transfer?batchId={}&toUser={}&receiverDid={}
<sample>
http://localhost:10060/transfer?batchId=JAJAJA&toUser=O=ConsumerOrganization,L=Rajasthan,C=IN&receiverDid=IJKL789


http://localhost:{}/my-batches
<sample>
http://localhost:10070/my-batches


http://localhost:{}/me
<sample>
http://localhost:10050/me



