package com.template.webserver;

import com.template.dto.DashboardDTO;
import com.template.flows.RegisterFlow;
import com.template.states.Register;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.node.services.vault.Sort;
import net.corda.core.node.services.vault.SortAttribute;
import net.corda.core.transactions.SignedTransaction;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);
    private String myLegalName;
    private final CordaX500Name me;

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !proxy.notaryIdentities()
                .stream().filter(el -> nodeInfo.isLegalIdentity(el))
                .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @GetMapping(value = "/templateendpoint", produces = "text/plain")
    private String templateendpoint() {
        return "Define an endpoint here.";
    }

    @GetMapping(value = "/me", produces = "application/json")
    private Object whoAmI() {
        return proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

//    @GetMapping(value = "/my-batches", produces = "application/json")
//    private ResponseEntity<List<StateAndRef<TemplateState>>> getMyBatches() {
//
//        Sort.SortColumn sortByUid = new Sort.SortColumn(new SortAttribute.Standard(Sort.LinearStateAttribute.UUID), Sort.Direction.DESC);
//        return ResponseEntity.ok(proxy.vaultQueryBy(new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL),
//                new PageSpecification(DEFAULT_PAGE_NUM, 200), new Sort(ImmutableSet.of(sortByUid)), TemplateState.class).getStates());
//    }

//    @GetMapping(value = "/get-states", produces = "application/json")
//     public ResponseEntity<List<StateAndRef<TemplateState>>> getStates() {
//        return ResponseEntity.ok(proxy.vaultQuery(TemplateState.class).getStates());
////          public List<StateAndRef<TemplateState>> getStates() {
////        return proxy.vaultQuery(TemplateState.class).getStates();
//    }
    @GetMapping("/attachments/{hash}")
    private ResponseEntity<Resource> downloadByHash(@PathVariable String hash)  {
        InputStreamResource inputStream = new InputStreamResource(proxy.openAttachment(SecureHash.parse(hash)));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename="+hash+".zip")
                .body(inputStream);
    }

    @PostMapping(value = "/upload", produces = "application/json")
    //private ResponseEntity<String> createRegister(@RequestBody  Register register, @PathVariable String toUser) {
    //Arquivo  Zipado
    private ResponseEntity<String> upload(@RequestParam MultipartFile file) throws IOException{
        String filename = file.getOriginalFilename();
        String uploader = this.me.getCommonName();
        if(filename == null) {
            return ResponseEntity.badRequest().body("File name must be set");
        }
        System.out.println("Arquivo recebido => "+filename);
        System.out.println("Uploader =>"+uploader);
        SecureHash hash = proxy.uploadAttachmentWithMetadata(file.getInputStream(), uploader, filename);

        // return ResponseEntity.status(HttpStatus.CREATED).body("Transaction id " + signedTransaction.get().getId() +" committed to l
        //return ResponseEntity.created(URI.create("attachments/"+hash)).body("Attachment uploaded with hash ->"+hash);
        return ResponseEntity.status(HttpStatus.CREATED).body(hash.toString());
    }

    private SecureHash uploadZip(InputStream inputStream, String uploader, String filename) throws IOException {
        String zipName = "$filename-${UUID.randomUUID()}.zip";
        FileOutputStream fos = new FileOutputStream(zipName);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        ZipEntry zipEntry = new ZipEntry(filename);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        System.out.println("Zipando Arquivo-->");
        while((length = inputStream.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
            System.out.println("****");
        }
        zipOut.close();
        inputStream.close();
        fos.close();

        //InputStream arquivo = new ZipInputStream(zipOut);


        return proxy.uploadAttachmentWithMetadata(inputStream, uploader, filename );
    }

    @GetMapping(value = "/my-batches", produces = "application/json")
    private ResponseEntity<ArrayList<Register>> getMyBatches() {
        ArrayList<Register> states = new ArrayList<Register>();
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
        Sort.SortColumn sortByUid = new Sort.SortColumn(new SortAttribute.Standard(Sort.LinearStateAttribute.UUID), Sort.Direction.DESC);
        List<StateAndRef<Register>> refStates = proxy.vaultQuery(Register.class).getStates();
        for(Integer i= 0; i < refStates.size(); i++){
            Register tmp = refStates.get(i).getState().getData();
            states.add(tmp);
        }
        return ResponseEntity.ok(states);
    }

    @GetMapping(value = "/register/{id}", produces = "application/json")
    private ResponseEntity<ArrayList<Register>> getRegister(@PathVariable String id) {
        ArrayList<Register> states = new ArrayList<Register>();

        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                null,
                Collections.singletonList(id),
                Vault.StateStatus.UNCONSUMED);//CONSUMED,UNCOSUMED,ALL

        List<StateAndRef<Register>> listStateAndRef = proxy.vaultQueryByCriteria( queryCriteria,Register.class).getStates();

        for(Integer i= 0; i < listStateAndRef.size(); i++){
            Register tmp = listStateAndRef.get(i).getState().getData();
            states.add(tmp);
        }
        return ResponseEntity.ok(states);
    }

    @GetMapping(value = "/dashboard", produces = "application/json")
    private ResponseEntity<?> getDashboard() {
        DashboardDTO dashboard = new DashboardDTO();

        List<Party> parties = proxy.nodeInfo().getLegalIdentities();

//            QueryCriteria countCriteriaTotal = new QueryCriteria.VaultCustomQueryCriteria(
//                    Builder.count(QueryCriteriaUtils.getField("registerId", Register.class)),
//                    Vault.StateStatus.UNCONSUMED);

//            Vault.Page<Register> totalRegistersCountPage =
//                    proxy.vaultQueryByCriteria(countCriteriaTotal, Register.class);

        //Get total contracts
        List<StateAndRef<Register>> refStates = proxy.vaultQuery(Register.class).getStates();
        int countContracts = refStates.size();

        //Get total contracts updated
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED);
        List<StateAndRef<Register>> totStates = proxy.vaultQueryByCriteria(generalCriteria,Register.class).getStates();
        int totUpdates = totStates.size();

        //Get total contracts value
        Double accContractValue = 0.0;
        for(int i = 0; i < refStates.size(); i++){
            Register tmp = refStates.get(i).getState().getData();
            JSONObject jsonObject = (JSONObject) JSONValue.parse(tmp.getPayload());
            if(jsonObject.get("total")!=null){
                String contractValue = jsonObject.get("total").toString();
                accContractValue = accContractValue + Double.parseDouble(contractValue);
            }
        }

        dashboard.setTotalContractValue(new BigDecimal(accContractValue.toString()));
        dashboard.setTotalContracts(countContracts);
        dashboard.setTotalContractUpdates(totUpdates);

        String jsonGraphTotalContractsByMonth = "[['Jan', 56],['Fev', 1256],['Mar', 658],['Abr', 852],['Mai', 2200]]";
        String jsonGraphTotalContractsUpdatedByMonth = "[['Jan', 10],['Fev', 20],['Mar', 4],['Abr', 3],['Mai', 0]]";
        dashboard.setBarChartTotalContractsByMonth(jsonGraphTotalContractsByMonth);
        dashboard.setBarChartTotalUpdateByMonth(jsonGraphTotalContractsUpdatedByMonth);

        return ResponseEntity.ok(dashboard);
    }


    @GetMapping(value = "/register/history/{id}", produces = "application/json")
    private ResponseEntity<ArrayList<Register>> getRegisterHistory(@PathVariable String id) {
        ArrayList<Register> states = new ArrayList<Register>();

        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                null,
                Collections.singletonList(id),
                Vault.StateStatus.ALL);//CONSUMED,UNCOSUMED,ALL

        List<StateAndRef<Register>> listStateAndRef = proxy.vaultQueryByCriteria( queryCriteria,Register.class).getStates();

        for(Integer i= 0; i < listStateAndRef.size(); i++){
            Register tmp = listStateAndRef.get(i).getState().getData();
            states.add(tmp);
        }
        return ResponseEntity.ok(states);
    }

    @GetMapping(value = "/peers", produces = "application/json")
    private ResponseEntity<HashMap<String, List<String>>> getPeers() {

        HashMap<String, List<String>> myMap = new HashMap<>();

        // Find all nodes that are not notaries, ourself, or the network map.
        Stream<NodeInfo> filteredNodes = proxy.networkMapSnapshot().stream()
                .filter(el -> !isNotary(el) && !isMe(el) && !isNetworkMap(el));
        // Get their names as strings
        List<String> nodeNames = filteredNodes.map(el -> el.getLegalIdentities().get(0).getName().toString())
                .collect(Collectors.toList());

        myMap.put("peers", nodeNames);

        return ResponseEntity.ok(myMap);
    }

    @PostMapping(value = "/register/{toUser}", produces = "application/json", headers = "Content-Type=application/json")
    private ResponseEntity<?> createRegister(@RequestBody Register register, @PathVariable String toUser) {

       System.out.println(register);
        if(register.getId() == null || register.getId().equals("")) {
            return ResponseEntity.badRequest().body("Id cannot be null");
        }

        //UUID uuid = UUID.fromString(id);

        CordaX500Name partyX500Name = CordaX500Name.parse(toUser);

       try{
            Party party = proxy.wellKnownPartyFromX500Name(partyX500Name);
            System.out.println("Party =>"+party.getName());
            if(party==null){
                return  ResponseEntity.badRequest().body("Party name "+party.getName()+"can not be found");
            }
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        Party party = proxy.wellKnownPartyFromX500Name(partyX500Name);

        register.setToUser(party);

        RegisterFlow registerFlow = new RegisterFlow(register);
        try{
           CordaFuture<SignedTransaction> signedTransaction = proxy.startFlowDynamic(registerFlow.getClass(), register).getReturnValue();

           return ResponseEntity.ok(null);

        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}