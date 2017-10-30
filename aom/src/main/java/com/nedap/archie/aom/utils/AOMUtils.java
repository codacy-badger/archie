package com.nedap.archie.aom.utils;

import com.google.common.base.Joiner;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.ArchetypeModelObject;
import com.nedap.archie.aom.CAttribute;
import com.nedap.archie.aom.CComplexObject;
import com.nedap.archie.definitions.AdlCodeDefinitions;
import com.nedap.archie.paths.PathSegment;
import com.nedap.archie.paths.PathUtil;
import com.nedap.archie.query.APathQuery;
import com.nedap.archie.rminfo.ModelInfoLookup;
import com.nedap.archie.rminfo.RMAttributeInfo;
import com.nedap.archie.rminfo.RMTypeInfo;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AOMUtils {

    private static Pattern idCodePAttern = Pattern.compile("(id|at|ac)(0|[1-9][0-9]*)(\\.(0|[1-9][0-9]*))*");

    public static int getSpecializationDepthFromCode(String code) {
        if(code == null) {
            return -1;
        } else if(code.indexOf(AdlCodeDefinitions.SPECIALIZATION_SEPARATOR) < 0) {
            return 0;
        } else {
            return StringUtils.countMatches(code, String.valueOf(AdlCodeDefinitions.SPECIALIZATION_SEPARATOR));
        }
    }

    public static boolean isIdCode(String code) {
        return code.startsWith(AdlCodeDefinitions.ID_CODE_LEADER);
    }

    public static boolean isValueCode(String code) {
        return code.startsWith(AdlCodeDefinitions.VALUE_CODE_LEADER);
    }

    public static boolean isValueSetCode(String code) {
        return code.startsWith(AdlCodeDefinitions.VALUE_SET_CODE_LEADER);
    }

    public static boolean isValidIdCode(String code) {
        return idCodePAttern.matcher(code).matches();
    }

    public static String pathAtSpecializationLevel(List<PathSegment> pathSegments, int level) {
        //todo: this doesn't clone the original
        for(PathSegment segment:pathSegments) {
            if(segment.getNodeId() != null && AOMUtils.isValidIdCode(segment.getNodeId()) && AOMUtils.getSpecializationDepthFromCode(segment.getNodeId()) > level) {
                segment.setNodeId(codeAtLevel(segment.getNodeId(), level));
            }
        }
        return PathUtil.getPath(pathSegments);
    }

    private static String codeAtLevel(String nodeId, int level) {
        NodeIdUtil nodeIdUtil = new NodeIdUtil(nodeId);
        List<Integer> codes = new ArrayList<>();
        for(int i = 0; i <= level;i++) {
            codes.add(nodeIdUtil.getCodes().get(i));
        }
        return nodeIdUtil.getPrefix() + Joiner.on(AdlCodeDefinitions.SPECIALIZATION_SEPARATOR).join(codes);

    }

    public static RedefinitionStatus getSpecialisationStatusFromCode(String nodeId, int integer) {
        return RedefinitionStatus.UNDEFINED;//TODO
    }

    public static ArchetypeModelObject getDifferentialPathFromParent(Archetype flatParent, CAttribute attributeWithDifferentialPath) {
        //adl workbench deviates from spec by only allowing differential paths at root, we allow them everywhere, according to spec
        ArchetypeModelObject parentAOMObject = flatParent.itemAtPath(pathAtSpecializationLevel(attributeWithDifferentialPath.getParent().getPathSegments(), flatParent.specializationDepth()));
        if (parentAOMObject != null && parentAOMObject instanceof CComplexObject) {
            CComplexObject parentObject = (CComplexObject) parentAOMObject;
            ArchetypeModelObject attributeInParent = parentObject.itemAtPath(attributeWithDifferentialPath.getDifferentialPath());
            return attributeInParent;
        }
        return null;
    }

    /**
     * Returns true if at least one [idx] predicate is present in the path
     * @param path
     * @return
     */
    public static boolean isArchetypePath(String path) {
        APathQuery query = new APathQuery(path);
        for(PathSegment segment:query.getPathSegments()) {
            if(segment.getNodeId() != null || segment.getArchetypeRef() != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasReferenceModelPath(ModelInfoLookup lookup, String rmTypeName, String path) {
        //cannot find the implementation in the eiffel ADL-tools, but I think it should be this:
        RMTypeInfo typeInfo = lookup.getTypeInfo(rmTypeName);
        APathQuery query = new APathQuery(path);
        for(PathSegment segment:query.getPathSegments()) {
            if(typeInfo == null) {
                return false;
            }
            RMAttributeInfo attribute = typeInfo.getAttribute(segment.getNodeName());
            if(attribute == null) {
                return false;
            }
            typeInfo = lookup.getTypeInfo(attribute.getTypeInCollection());//TODO: this should not work on class objects
        }
        return true;
    }

    //check if the last node id in the path has a bigger specialization level than the specialization level of the parent
    //but it does a little loop to check if it happens somewhere else as well. ok...
    public static boolean isPhantomPathAtLevel(List<PathSegment> pathSegments, int specializationDepth) {
        for(int i = pathSegments.size()-1; i >=0; i--) {
            String nodeId = pathSegments.get(i).getNodeId();
            if(nodeId != null && AOMUtils.isValidIdCode(nodeId) && specializationDepth < AOMUtils.getSpecializationDepthFromCode(nodeId)) {
                return codeExistsAtLevel(nodeId, specializationDepth);
            }
        }
        return false;
    }

    public static boolean codeExistsAtLevel(String nodeId, int specializationDepth) {
        NodeIdUtil nodeIdUtil = new NodeIdUtil(nodeId);
        int specializationDepthOfCode = AOMUtils.getSpecializationDepthFromCode(nodeId);
        if(specializationDepth > specializationDepthOfCode) {
            String code = "";
            for(int i = 0; i <= specializationDepth; i++) {
                code += nodeIdUtil.getCodes().get(i);
            }
            return Integer.parseInt(code) > 0;
        }
        return false;
    }
}
