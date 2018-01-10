package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.vdscommands.CreateVDSCommandParameters;
import org.ovirt.engine.core.di.Injector;
import org.ovirt.engine.core.utils.XmlUtils;
import org.ovirt.engine.core.vdsbroker.builder.vminfo.LibvirtVmXmlBuilder;
import org.ovirt.engine.core.vdsbroker.builder.vminfo.VmInfoBuildUtils;
import org.ovirt.engine.core.vdsbroker.builder.vminfo.VmInfoBuilder;
import org.ovirt.engine.core.vdsbroker.builder.vminfo.VmInfoBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateBrokerVDSCommand<P extends CreateVDSCommandParameters> extends VmReturnVdsBrokerCommand<P> {

    private static final Logger log = LoggerFactory.getLogger(CreateBrokerVDSCommand.class);

    protected VM vm;

    @Inject
    private VmInfoBuildUtils vmInfoBuildUtils;
    @Inject
    private SysprepHandler sysprepHandler;

    public CreateBrokerVDSCommand(P parameters) {
        super(parameters, parameters.getVm().getId());
        vm = parameters.getVm();
    }

    @Override
    protected void executeVdsBrokerCommand() {
        vmReturn = getBroker().create(createInfo());
        proceedProxyReturnValue();
        VdsBrokerObjectsBuilder.updateVMDynamicData(vm.getDynamicData(),
                vmReturn.vm, getVds());
    }

    private Map<String, Object> createInfo() {
        if (FeatureSupported.isDomainXMLSupported(vm.getClusterCompatibilityVersion())) {
            String engineXml = generateDomainXml();
            String hibernationVolHandle = getParameters().getHibernationVolHandle();
            if (StringUtils.isEmpty(hibernationVolHandle)) {
                return Collections.singletonMap(VdsProperties.engineXml, engineXml);
            } else {
                Map<String, Object> createInfo = new HashMap<>(2);
                createInfo.put(VdsProperties.engineXml, engineXml);
                createInfo.put(VdsProperties.hiberVolHandle, hibernationVolHandle);
                return createInfo;
            }
        } else {
            Map<String, Object> createInfo = new HashMap<>();
            VmInfoBuilder builder = createBuilder(createInfo);
            buildVmData(builder);
            log.info("VM {}", createInfo);
            return createInfo;
        }
    }

    private String generateDomainXml() {
        LibvirtVmXmlBuilder builder = Injector.injectMembers(new LibvirtVmXmlBuilder(
                vm,
                getVds().getId(),
                getPayload(),
                getVds().getCpuThreads(),
                getParameters().isVolatileRun(),
                getParameters().getPassthroughVnicToVfMap()));
        String libvirtXml = builder.buildCreateVm();
        String prettyLibvirtXml = XmlUtils.prettify(libvirtXml);
        if (prettyLibvirtXml != null) {
            log.info("VM {}", prettyLibvirtXml);
        }
        return libvirtXml;
    }

    private VmInfoBuilder createBuilder(Map<String, Object> createInfo) {
        final VmInfoBuilderFactory vmInfoBuilderFactory = Injector.get(VmInfoBuilderFactory.class);
        return vmInfoBuilderFactory.createVmInfoBuilder(vm, getParameters().getVdsId(), createInfo);
    }

    private void buildVmData(VmInfoBuilder builder) {
        builder.buildVmProperties(getParameters().getHibernationVolHandle());
        builder.buildVmVideoCards();
        builder.buildVmGraphicsDevices();
        builder.buildVmCD(getParameters().getVmPayload());
        builder.buildVmFloppy(getParameters().getVmPayload());
        builder.buildVmDrives();
        builder.buildVmNetworkInterfaces(getParameters().getPassthroughVnicToVfMap());
        builder.buildVmNetworkCluster();
        builder.buildVmBootSequence();
        builder.buildVmBootOptions();
        builder.buildVmSoundDevices();
        builder.buildVmConsoleDevice();
        builder.buildVmTimeZone();
        builder.buildVmUsbDevices();
        builder.buildVmMemoryBalloon();
        builder.buildVmWatchdog();
        builder.buildVmVirtioScsi();
        builder.buildVmVirtioSerial();
        builder.buildVmRngDevice();
        builder.buildUnmanagedDevices(getParameters().getHibernationVolHandle());
        builder.buildVmSerialNumber();
        builder.buildVmNumaProperties();
        builder.buildVmHostDevices();

        switch (getParameters().getInitializationType()) {
        case Sysprep:
            String sysPrepContent = sysprepHandler.getSysPrep(
                    getParameters().getVm(),
                    getParameters().getSysPrepParams());

            if (!"".equals(sysPrepContent)) {
                builder.buildSysprepVmPayload(sysPrepContent);
            }
            break;

        case CloudInit:
            CloudInitHandler cloudInitHandler = new CloudInitHandler(getParameters().getVm().getVmInit());
            Map<String, byte[]> cloudInitContent;
            try {
                cloudInitContent = cloudInitHandler.getFileData();
            } catch (Exception e) {
                throw new RuntimeException("Failed to build cloud-init data:", e);
            }

            if (cloudInitContent != null && !cloudInitContent.isEmpty()) {
                builder.buildCloudInitVmPayload(cloudInitContent);
            }
            break;

        case None:
        }
    }

    private VmDevice getPayload() {
        switch (getParameters().getInitializationType()) {
        case Sysprep:
            String sysPrepContent = sysprepHandler.getSysPrep(
                    getParameters().getVm(),
                    getParameters().getSysPrepParams());

            return (!"".equals(sysPrepContent)) ?
                    vmInfoBuildUtils.createSysprepPayloadDevice(sysPrepContent, getParameters().getVm())
                    : null;

        case CloudInit:
            CloudInitHandler cloudInitHandler = new CloudInitHandler(getParameters().getVm().getVmInit());
            Map<String, byte[]> cloudInitContent;
            try {
                cloudInitContent = cloudInitHandler.getFileData();
            } catch (Exception e) {
                throw new RuntimeException("Failed to build cloud-init data:", e);
            }

            return (cloudInitContent != null && !cloudInitContent.isEmpty()) ?
                    vmInfoBuildUtils.createCloudInitPayloadDevice(cloudInitContent, getParameters().getVm())
                    : null;

        case None:
        default:
            return getParameters().getVmPayload();
        }
    }
}
