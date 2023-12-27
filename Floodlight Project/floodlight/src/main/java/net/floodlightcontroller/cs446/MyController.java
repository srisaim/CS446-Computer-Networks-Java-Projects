package net.floodlightcontroller.cs446;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.util.FlowModUtils;
import net.floodlightcontroller.util.OFMessageDamper;
import net.floodlightcontroller.util.OFMessageUtils;

public class MyController implements IOFMessageListener, IFloodlightModule {

    protected IFloodlightProviderService floodlightProvider;

    protected static Logger logger;

    // This is for alternating between different paths.
    private boolean usePathS1S2S4 = true;

    protected OFMessageDamper messageDamper;
    private int OFMESSAGE_DAMPER_CAPACITY = 10000;
    private int OFMESSAGE_DAMPER_TIMEOUT = 250; // ms
    public static final int FORWARDING_APP_ID = 446;
    static {
        AppCookie.registerApp(FORWARDING_APP_ID, "forwarding");
    }
    protected static final U64 cookie = U64.of(0xABCD << 48);//AppCookie.makeCookie(FORWARDING_APP_ID, 0);
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return MyController.class.getSimpleName();
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        // TODO Auto-generated method stub
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        // TODO Auto-generated method stub
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        logger = LoggerFactory.getLogger(MyController.class);

        messageDamper = new OFMessageDamper(OFMESSAGE_DAMPER_CAPACITY,
                EnumSet.of(OFType.FLOW_MOD),
                OFMESSAGE_DAMPER_TIMEOUT);

    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        // TODO Auto-generated method stub
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);

    }

    @Override
    public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
                                                                   FloodlightContext cntx) {
        switch (msg.getType()) {
            case PACKET_IN:
                Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
                MacAddress srcMac = eth.getSourceMACAddress();
                MacAddress dstMac = eth.getDestinationMACAddress();

                if (eth.getEtherType() == EthType.IPv4) {
                    IPv4 ipv4 = (IPv4) eth.getPayload();
                    if (ipv4.getProtocol() == IpProtocol.ICMP) {
                        logger.info("ICMP Packet In from switch {}, src MAC:{}, src IP:{}",
                                sw.getId().toString(),
                                srcMac.toString());

                        String switchId = sw.getId().toString();
                        int inPortNumber = ((OFPacketIn) msg).getInPort().getPortNumber();

                        if (switchId.contains("01")) {
                            addMyFlow1(sw, eth, inPortNumber);
                            usePathS1S2S4 = !usePathS1S2S4;
                        } else if (switchId.contains("02")) {
                            addMyFlow2(sw, eth, inPortNumber);
                        } else if (switchId.contains("03")) {
                            addMyFlow3(sw, eth, inPortNumber);
                        } else if (switchId.contains("04")) {
                            addMyFlow4(sw, eth, inPortNumber);
                        }
                    }
                }
                break;
            default:
                break;
        }
        return Command.CONTINUE;
    }

    void addMyFlow1(IOFSwitch sw, Ethernet eth, int port) {
        if (port != 1 && port != 2) return;
        int outPort = usePathS1S2S4 ? 2 : 3;

        OFFactory myFactory = sw.getOFFactory();
        OFFlowMod.Builder fmb = myFactory.buildFlowAdd();

        IPv4 ipv4 = (IPv4) eth.getPayload();
        Match myMatch = myFactory.buildMatch()
                .setExact(MatchField.IN_PORT, OFPort.of(port))
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.ETH_SRC, eth.getSourceMACAddress())
                .setExact(MatchField.ETH_DST, eth.getDestinationMACAddress())
                .setExact(MatchField.IPV4_SRC, ipv4.getSourceAddress())
                .setExact(MatchField.IPV4_DST, ipv4.getDestinationAddress())
                .build();

        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
        OFActions actions = myFactory.actions();

        OFAction output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(OFPort.of(outPort))
                .build();

        actionList.add(output);

        fmb.setIdleTimeout(5)
                .setBufferId(OFBufferId.NO_BUFFER)
                .setCookie(U64.of(0))
                .setPriority(FlowModUtils.PRIORITY_HIGH)
                .setMatch(myMatch)
                .setActions(actionList);

        try {
            messageDamper.write(sw, fmb.build());
        } catch (Exception e) {
            logger.error("Failed to write flow for addMyFlow1", e);
        }
    }

    void addMyFlow2(IOFSwitch sw, Ethernet eth, int port) {
        if (port != 1 && port != 2) return;

        OFFactory myFactory = sw.getOFFactory();
        OFFlowMod.Builder fmb = myFactory.buildFlowAdd();

        IPv4 ipv4 = (IPv4) eth.getPayload();
        Match myMatch = myFactory.buildMatch()
                .setExact(MatchField.IN_PORT, OFPort.of(port))
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.ETH_SRC, eth.getSourceMACAddress())
                .setExact(MatchField.ETH_DST, eth.getDestinationMACAddress())
                .setExact(MatchField.IPV4_SRC, ipv4.getSourceAddress())
                .setExact(MatchField.IPV4_DST, ipv4.getDestinationAddress())
                .build();

        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
        OFActions actions = myFactory.actions();

        OFAction output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(OFPort.of(2))
                .build();

        actionList.add(output);

        fmb.setIdleTimeout(5)
                .setBufferId(OFBufferId.NO_BUFFER)
                .setCookie(U64.of(0))
                .setPriority(FlowModUtils.PRIORITY_HIGH)
                .setMatch(myMatch)
                .setActions(actionList);

        try {
            messageDamper.write(sw, fmb.build());
        } catch (Exception e) {
            logger.error("Failed to write flow for addMyFlow2", e);
        }
    }

    void addMyFlow3(IOFSwitch sw, Ethernet eth, int port) {
        if (port != 1 && port != 2) return;

        OFFactory myFactory = sw.getOFFactory();
        OFFlowMod.Builder fmb = myFactory.buildFlowAdd();

        IPv4 ipv4 = (IPv4) eth.getPayload();
        Match myMatch = myFactory.buildMatch()
                .setExact(MatchField.IN_PORT, OFPort.of(port))
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.ETH_SRC, eth.getSourceMACAddress())
                .setExact(MatchField.ETH_DST, eth.getDestinationMACAddress())
                .setExact(MatchField.IPV4_SRC, ipv4.getSourceAddress())
                .setExact(MatchField.IPV4_DST, ipv4.getDestinationAddress())
                .build();

        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
        OFActions actions = myFactory.actions();

        OFAction output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(OFPort.of(2))
                .build();

        actionList.add(output);

        fmb.setIdleTimeout(5)
                .setBufferId(OFBufferId.NO_BUFFER)
                .setCookie(U64.of(0))
                .setPriority(FlowModUtils.PRIORITY_HIGH)
                .setMatch(myMatch)
                .setActions(actionList);

        try {
            messageDamper.write(sw, fmb.build());
        } catch (Exception e) {
            logger.error("Failed to write flow for addMyFlow3", e);
        }
    }

    void addMyFlow4(IOFSwitch sw, Ethernet eth, int port) {
        if((port != 1) && (port != 3)) return;

        int outPort;

        if (port == 1) {
            outPort = 3;
        } else if (port == 2) {
            outPort = 3;
        } else {
            throw new UnsupportedOperationException("Unexpected inPort for addMyFlow4: " + port);
        }

        OFFactory myFactory = sw.getOFFactory();
        OFFlowMod.Builder fmb = myFactory.buildFlowAdd();

        IPv4 ipv4 = (IPv4) eth.getPayload();
        Match myMatch = myFactory.buildMatch()
                .setExact(MatchField.IN_PORT, OFPort.of(port))
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.ETH_SRC, eth.getSourceMACAddress())
                .setExact(MatchField.ETH_DST, eth.getDestinationMACAddress())
                .setExact(MatchField.IPV4_SRC, ipv4.getSourceAddress())
                .setExact(MatchField.IPV4_DST, ipv4.getDestinationAddress())
                .build();

        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
        OFActions actions = myFactory.actions();

        OFAction output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(OFPort.of(outPort))
                .build();

        actionList.add(output);

        fmb.setIdleTimeout(5)
                .setBufferId(OFBufferId.NO_BUFFER)
                .setCookie(U64.of(0))
                .setPriority(FlowModUtils.PRIORITY_HIGH)
                .setMatch(myMatch)
                .setActions(actionList);

        try {
            messageDamper.write(sw, fmb.build());
        } catch (Exception e) {
            logger.error("Failed to write flow for addMyFlow4", e);
        }
    }
}

