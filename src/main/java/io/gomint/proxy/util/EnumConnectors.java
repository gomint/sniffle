package io.gomint.proxy.util;

import io.gomint.proxy.inventory.Material;
import io.gomint.proxy.inventory.MaterialMagicNumbers;

/**
 * @author geNAZt
 * @version 1.0
 */
public class EnumConnectors {

    public static final EnumConnector<Material, MaterialMagicNumbers> MATERIAL_CONNECTOR = new EnumConnector<>( Material.class, MaterialMagicNumbers.class );

}
