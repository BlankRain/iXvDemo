package com.sky.vdk.vlx.generator

import com.sky.vdk.vlx.generator.nodecfg.NodeConfig
import com.sky.vdk.vlx.generator.nodedata.EndNodeBean
import com.sky.vdk.vlx.generator.nodedata.LinkNodeBean
import com.sky.vdk.vlx.generator.nodedata.NodeBean
import com.sky.vdk.vlx.generator.utils.SkyXMLUtils
import org.apache.commons.io.FileUtils
import org.apache.log4j.Logger
import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Element

/**
 * Created with IntelliJ IDEA.
 * User: yixian
 * Date: 13-11-6
 * Time: 下午4:20
 */
class VLXGenerator {

    private Logger logger = Logger.getLogger(getClass());
    static def endConfigs = [:];
    static def linkConfigs = [:];
    def endNodes = [:];
    def linkNodes = [:];
    private static Document document;

    /**
     * 初始化
     */
    {
        VLXModelInitor initor = VLXModelInitor.getInstance();
        document = DocumentHelper.parseText(initor.baseVLX);
        loadNodeConfig();
    }

    void reset() {
        endNodes = [:];
        linkNodes = [:];
    }

    private void loadNodeConfig() {
        logger.debug("loading node configs,document is null:" + (document == null));
        if (document != null) {
            def endTypesNode = document.selectSingleNode("vlx:vlx/typeCatalogue/endTypes") as Element;
            def linkTypesNode = document.selectSingleNode("vlx:vlx/typeCatalogue/linkTypes") as Element;
            loadTypes(endTypesNode, "end");
            loadTypes(linkTypesNode, "link");
        }
        displayConfigs();
    }

    private void displayConfigs() {
        logger.info("loaded config:");
        endConfigs.each { def entry ->
            logger.info(entry.getValue().toString());
        }
        linkConfigs.each { def entry ->
            logger.info(entry.getValue().toString());
        }
    }

    private void loadTypes(Element nodeList, String nodeType) {
        def nodeConfigs = nodeType == "end" ? endConfigs : linkConfigs;
        for (Iterator ite = nodeList.elementIterator(); ite.hasNext();) {
            Element element = ite.next() as Element;
            logger.debug("load element:" + element.getName());
            String localName = element.attributeValue("localName");
            def properties = loadElementProperties(element);
            nodeConfigs[localName] = NodeConfig.createInstance(nodeType, localName, properties);
        }
    }

    private static def loadElementProperties(Element element) {
        def eleProperties = [];
        for (Iterator propertyIte = element.elementIterator("property"); propertyIte.hasNext();) {
            Element propertyElement = propertyIte.next() as Element;
            eleProperties.add(propertyElement.attributeValue("localName"));
        }
        return eleProperties;
    }

    /**
     * 根据catType取得End节点的配置
     * @param name
     * @return
     */
    NodeConfig getEndConfig(String name) {
        return endConfigs[name] as NodeConfig;
    }

    /**
     * 根据catType取得Link节点的配置
     * @param name
     * @return
     */
    NodeConfig getLinkConfig(String name) {
        return linkConfigs[name] as NodeConfig;
    }

    /**
     * 添加End节点
     * @param catType
     * @param rowData
     * @return
     */
    EndNodeBean addEnd(String catType, def rowData) {
        def endNode = getEndConfig(catType)?.newNode(rowData) as EndNodeBean;
        endNodes[endNode.getNodeId()] = endNode;
        return endNode;
    }

    /**
     * 关联两个节点
     * @param catType 节点类型
     * @param end1 起始节点
     * @param end2 结束节点
     * @param params 连接参数
     * @return
     */
    LinkNodeBean connectNodes(String catType, EndNodeBean end1, EndNodeBean end2, def params) {
        def linkNode = getLinkConfig(catType)?.newNode(params) as LinkNodeBean;
        linkNode.linkNodes(end1, end2);
        linkNodes[linkNode.getNodeId()] = linkNode;
        return linkNode;
    }

    /**
     * 生成最终XML字符串
     * @return
     */
    String generateVLX() {
        def copyDoc = document.clone() as Document;
        buildEndNodes(copyDoc);
        buildLinkNodes(copyDoc);
        return SkyXMLUtils.formatXMLOutput(copyDoc);
    }

    /**
     * 将最终生成的XML字符串写入文件
     * @param filePath
     */
    void generateToFile(String filePath) {
        FileUtils.write(new File(filePath), generateVLX(), "utf-8");
    }

    private void buildEndNodes(Document doc) {
        def ends = doc.selectSingleNode("vlx:vlx/content/ends") as Element;
        endNodes.each { def entry ->
            NodeBean nodeInfo = entry.getValue() as NodeBean;
            nodeInfo.buildElement(ends);
        }
    }

    private void buildLinkNodes(Document doc) {
        def links = doc.selectSingleNode("vlx:vlx/content/links") as Element;
        linkNodes.each { def entry ->
            NodeBean nodeInfo = entry.getValue() as NodeBean;
            nodeInfo.buildElement(links);
        }
    }

}