//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2016.08.31 um 10:07:11 AM CEST 
//


package eu.transkribus.core.model.beans.pagecontent;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 * 				Indexed group containing ordered elements
 * 			
 * 
 * <p>Java-Klasse für OrderedGroupIndexedType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="OrderedGroupIndexedType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="RegionRefIndexed" type="{http://schema.primaresearch.org/PAGE/gts/pagecontent/2013-07-15}RegionRefIndexedType"/>
 *         &lt;element name="OrderedGroupIndexed" type="{http://schema.primaresearch.org/PAGE/gts/pagecontent/2013-07-15}OrderedGroupIndexedType"/>
 *         &lt;element name="UnorderedGroupIndexed" type="{http://schema.primaresearch.org/PAGE/gts/pagecontent/2013-07-15}UnorderedGroupIndexedType"/>
 *       &lt;/choice>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="index" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="caption" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OrderedGroupIndexedType", propOrder = {
    "regionRefIndexedOrOrderedGroupIndexedOrUnorderedGroupIndexed"
})
public class OrderedGroupIndexedType {

    @XmlElements({
        @XmlElement(name = "RegionRefIndexed", type = RegionRefIndexedType.class),
        @XmlElement(name = "OrderedGroupIndexed", type = OrderedGroupIndexedType.class),
        @XmlElement(name = "UnorderedGroupIndexed", type = UnorderedGroupIndexedType.class)
    })
    protected List<Object> regionRefIndexedOrOrderedGroupIndexedOrUnorderedGroupIndexed;
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAttribute(name = "index", required = true)
    protected int index;
    @XmlAttribute(name = "caption")
    protected String caption;

    /**
     * Gets the value of the regionRefIndexedOrOrderedGroupIndexedOrUnorderedGroupIndexed property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the regionRefIndexedOrOrderedGroupIndexedOrUnorderedGroupIndexed property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRegionRefIndexedOrOrderedGroupIndexedOrUnorderedGroupIndexed().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RegionRefIndexedType }
     * {@link OrderedGroupIndexedType }
     * {@link UnorderedGroupIndexedType }
     * 
     * 
     */
    public List<Object> getRegionRefIndexedOrOrderedGroupIndexedOrUnorderedGroupIndexed() {
        if (regionRefIndexedOrOrderedGroupIndexedOrUnorderedGroupIndexed == null) {
            regionRefIndexedOrOrderedGroupIndexedOrUnorderedGroupIndexed = new ArrayList<Object>();
        }
        return this.regionRefIndexedOrOrderedGroupIndexedOrUnorderedGroupIndexed;
    }

    /**
     * Ruft den Wert der id-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Legt den Wert der id-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Ruft den Wert der index-Eigenschaft ab.
     * 
     */
    public int getIndex() {
        return index;
    }

    /**
     * Legt den Wert der index-Eigenschaft fest.
     * 
     */
    public void setIndex(int value) {
        this.index = value;
    }

    /**
     * Ruft den Wert der caption-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCaption() {
        return caption;
    }

    /**
     * Legt den Wert der caption-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCaption(String value) {
        this.caption = value;
    }

}
