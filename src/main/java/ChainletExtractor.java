import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.script.ScriptPattern;
import org.bitcoinj.utils.BlockFileLoader;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ChainletExtractor {

    // Location of block files. This is where your blocks are located.
    // Check the documentation of Bitcoin Core if you are using
    // it, or use any other directory with blk*dat files.
    static String BitcoinBlockDirectory = "C:/Users/etr/AppData/Roaming/Bitcoin/blocks/";
    static NetworkParameters np = new MainNetParams();

    // A simple method with everything in it
    public static ArrayList<BlockInfoMap> computeBlockInfoMaps(Timestamp lastSeenBlockDate, int datFileHint) throws SQLException, ClassNotFoundException {
        // Just some initial setup

        Context.getOrCreate(MainNetParams.get());


        // We create a BlockFileLoader object by passing a list of files.
        // The list of files is built with the method buildList(), see
        // below for its definition.
        // extract latest blocks
        //BlockFileLoader loader = new BlockFileLoader(np,lastFile());
        // or extract all blocks
        BlockFileLoader loader = new BlockFileLoader(np, buildList(datFileHint));

        // bitcoinj does all the magic: from the list of files in the loader
        // it builds a list of blocks. We iterate over it using the following
        // for loop
        ArrayList<BlockInfoMap> blockInfoMap = new ArrayList<BlockInfoMap>();
        for (Block block : loader) {

            Timestamp blockTime = new Timestamp(block.getTime().getTime());
            if (lastSeenBlockDate != null && blockTime.compareTo(lastSeenBlockDate) <= 0) {
                //we have these blocks in the database already
                continue;
            }
            String blockHash = block.getHashAsString();
            String parentHash = block.getPrevBlockHash().toString();
            BlockInfoMap blockInfo = parseTransactions(block.getTransactions());
            blockInfo.setBlockHash(blockHash);
            blockInfo.setParentHash(parentHash);
            blockInfo.setTimestamp(blockTime);
            blockInfoMap.add(blockInfo);
        } // End of iteration over blocks
        return blockInfoMap;
    }




    static String extractOutputAddress(Script script) {

        String address;
        if (ScriptPattern.isP2PKH(script) || ScriptPattern.isP2WPKH(script)
                || ScriptPattern.isP2SH(script))
            address = script.getToAddress(np).toString();
        else if (ScriptPattern.isP2PK(script))
            address = byteArrayToHex(ScriptPattern.extractKeyFromP2PK(script));
        else if (ScriptPattern.isSentToMultisig(script))
            address = "unknownmultisig";
        else if (ScriptPattern.isWitnessCommitment(script)) {
            address = "SegWit";
        } else address = "unknown";

        return address;
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    static BlockInfoMap parseTransactions(List<Transaction> transactions) {
        BlockInfoMap infoMap = new BlockInfoMap();
        for (Transaction tr : transactions) {
            try {
                if (tr.isCoinBase()) {
                    for (TransactionOutput output : tr.getOutputs()) {
                        String address = extractOutputAddress(output.getScriptPubKey());
                        infoMap.addMinerAddress(address, output.getValue().getValue());
                    }
                }
                List<TransactionInput> inputs = tr.getInputs();
                List<TransactionOutput> outputs = tr.getOutputs();
                int inputSize = inputs.size();
                int outputSize = outputs.size();
                infoMap.addOccurrence(inputSize, outputSize);
                infoMap.addAmount(inputSize, outputSize, tr.getOutputSum().getValue());
                infoMap.addFee(tr.getFee());
            } catch (ScriptException e) {
                System.out.println("Skipping "+tr.hashCode()+ " due to script errors: "+tr.toString());
            }
        }
        return infoMap;
    }


    // The method returns a list of files in a directory according to a certain
    // pattern (block files have name blkNNNNN.dat)
    static List<File> buildList(int datFileHint) {
        List<File> list = new LinkedList<File>();
        for (int i = datFileHint; true; i++) {
            File file = new File(BitcoinBlockDirectory + String.format(Locale.US, "blk%05d.dat", i));
            if (!file.exists()) {
                break;
            }
            list.add(file);
        }
        return list;
    }

    static List<File> lastFile() {
        File lastFile = null;
        for (int i = 0; true; i++) {
            File file = new File(BitcoinBlockDirectory + String.format(Locale.US, "blk%05d.dat", i));
            if (!file.exists()) {
                break;
            }
            lastFile = file;
        }
        ArrayList<File> f = new ArrayList<File>();
        f.add(lastFile);
        return f;
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException, InterruptedException {
        int datFileHint = 2440;

        while (true) {
            Timestamp latestSeenBlockDate = null;
            if(latestSeenBlockDate==null){
                datFileHint=0;
                latestSeenBlockDate = Timestamp.valueOf(LocalDateTime.now().minusDays(5000));
            }
            System.out.println("We will start parsing from date " + latestSeenBlockDate.toString());

            ArrayList<BlockInfoMap> blockInfoMap = computeBlockInfoMaps(latestSeenBlockDate, datFileHint);

            TimeUnit.MINUTES.sleep(3);
        }
    }

}