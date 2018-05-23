package com.example.jsonparser;

import android.util.JsonReader;
import android.util.JsonToken;

import java.io.IOException;
import java.io.Reader;

/**
 * @author wuxio 2018-05-14:9:47
 */
public class JsonParser {

    //============================ 上一次操作 ============================

    /**
     * 定义操作
     */
    private static final int TOKEN_VALUE        = 1;
    private static final int TOKEN_NAME         = 3;
    private static final int TOKEN_BEGIN_OBJECT = 5;
    private static final int TOKEN_END_OBJECT   = 7;
    private static final int TOKEN_BEGIN_ARRAY  = 11;
    private static final int TOKEN_END_ARRAY    = 13;

    //============================ construct ============================


    public JsonParser() {

        mNumberType = Node.VALUE_NUMBER_FLOAT;
    }

    //============================ 配置使用什么类型保存数字类型 ============================

    private int mNumberType;


    public void setNumberType(@Node.NumberType int numberType) {

        this.mNumberType = numberType;
    }

    //============================ 当node读取数字类型的值失败的时候返回的值 ============================

    /**
     * 当node读取数字类型的值失败的时候返回的值,{@link Node#getInt()}
     */
    public static int errorNumber = -120;


    public static void setErrorNumber(int errorNumber) {

        errorNumber = errorNumber;
    }

    //============================ 解析Json ============================


    /**
     * @param reader 解析json
     */
    public ObjectNodeTree parse(Reader reader) {

        return parse(reader, false);
    }


    /**
     * @param reader 解析格式不标准的json
     */
    public ObjectNodeTree parseLenient(Reader reader) {

        return parse(reader, true);
    }


    private ObjectNodeTree parse(Reader reader, boolean lenient) {

        JsonReader jsonReader = new JsonReader(reader);

        jsonReader.setLenient(lenient);

        Node mCurrentNode = null;
        int lastToken = 0;
        ValueContainer mValueContainer = null;

        try {

            JsonToken peek = jsonReader.peek();
            while (peek != JsonToken.END_DOCUMENT) {

                switch (peek) {

                    case BEGIN_OBJECT:

                        /* BEGIN_OBJECT 主要工作: 1.初始化; 2.当前的节点设为Object子节点 */

                        jsonReader.beginObject();

                        if (lastToken == 0) {

                            /* 开始解析 */

                            /* 初始化一个json value容器,用来管理json的key对应的值 */

                            mValueContainer = new ValueContainer();

                            /* 初始化一个node,她的类型是类型是NODE_OBJECT
                            对应的值是ObjectNodeTree,该ObjectNodeTree对应整个Json */

                            mCurrentNode = new Node(mValueContainer);
                            ObjectNodeTree nodeTree = new ObjectNodeTree();
                            mCurrentNode.asObjectNode(nodeTree);

                            lastToken = TOKEN_BEGIN_OBJECT;
                            break;
                        }

                        if (lastToken == TOKEN_NAME) {

                            /* 上一个操作是TOKEN_NAME,现在是BEGIN_OBJECT,说明需要一个与当前node同级的object node */

                            ObjectNodeTree nodeTree = new ObjectNodeTree();
                            mCurrentNode.asObjectNode(nodeTree);

                        } else if (lastToken == TOKEN_BEGIN_ARRAY) {

                            /* 上一步操作是BEGIN_ARRAY 说明当前节点是 json array; 并且它的元素都是object类型的 */

                            ArrayNodeTree array = mCurrentNode.getArray();

                            Node node = new Node(mValueContainer);
                            ObjectNodeTree objectNodeTree = new ObjectNodeTree();
                            objectNodeTree.node = node;
                            node.asObjectNode(objectNodeTree);
                            node.parent = array;

                            array.addNode(node);

                            mCurrentNode = node;

                        } else if (lastToken == TOKEN_END_OBJECT) {

                            /* 上一步操作是END_OBJECT 说明当前节点是 json array; 并且它的元素都是object类型的 */

                            ArrayNodeTree array = (ArrayNodeTree) mCurrentNode.parent;

                            Node node = new Node(mValueContainer);
                            ObjectNodeTree objectNodeTree = new ObjectNodeTree();
                            objectNodeTree.node = node;
                            node.asObjectNode(objectNodeTree);
                            node.parent = array;

                            array.addNode(node);

                            mCurrentNode = node;
                        }

                        lastToken = TOKEN_BEGIN_OBJECT;
                        break;

                    case BEGIN_ARRAY:

                        jsonReader.beginArray();

                        if (lastToken == TOKEN_NAME) {

                            /* name --> BEGIN_ARRAY ,说明name那里创建的节点是 json array 节点 */

                            ArrayNodeTree arrayNodeTree = new ArrayNodeTree();
                            mCurrentNode.asArray(arrayNodeTree);
                        }

                        lastToken = TOKEN_BEGIN_ARRAY;
                        break;

                    case NAME:

                        String currentNodeName = jsonReader.nextName();

                        if (lastToken == TOKEN_BEGIN_OBJECT) {

                            /* 从 BEGIN_OBJECT 转到 NAME, 说明 BEGIN_OBJECT 创建的节点需要添加子节点 */

                            ObjectNodeTree nodeTree = mCurrentNode.getObject();
                            Node node = new Node(mValueContainer);
                            node.name = currentNodeName;
                            node.parent = nodeTree;
                            nodeTree.addNode(currentNodeName, node);

                            /* name 肯定对应一个值/一个json object/一个json数组,
                            所以需要将当前节点指向新创建的node,以便后续对该节点操作 */

                            mCurrentNode = node;

                        } else {

                            /* 只要上一个操作不是 BEGIN_OBJECT ,那么此处就为当前节点添加同级节点 */

                            ObjectNodeTree parent = (ObjectNodeTree) mCurrentNode.parent;
                            Node node = new Node(mValueContainer);
                            node.name = currentNodeName;
                            node.parent = parent;
                            parent.addNode(currentNodeName, node);

                            mCurrentNode = node;
                        }

                        lastToken = TOKEN_NAME;
                        break;

                    /* read to a value */

                    case NULL:

                        jsonReader.nextNull();

                        if (lastToken == TOKEN_NAME) {

                            mCurrentNode.asNullValueNode();

                        } else if (lastToken == TOKEN_BEGIN_ARRAY || lastToken == TOKEN_VALUE) {

                            /* 上一步操作是BEGIN_ARRAY/TOKEN_VALUE ,说明当前节点是 json array; 并且它的元素都是值类型的 */
                            ArrayNodeTree array = mCurrentNode.getArray();
                            Node node = new Node(mValueContainer);
                            node.asNullValueNode();
                            node.parent = array;
                            array.addNode(node);
                        }

                        lastToken = TOKEN_VALUE;
                        break;

                    case STRING:

                        String string = jsonReader.nextString();

                        if (lastToken == TOKEN_NAME) {

                            mCurrentNode.asStringValueNode(string);

                        } else if (lastToken == TOKEN_BEGIN_ARRAY || lastToken == TOKEN_VALUE) {

                            /* 上一步操作是BEGIN_ARRAY/TOKEN_VALUE ,说明当前节点是 json array; 并且它的元素都是值类型的 */
                            ArrayNodeTree array = mCurrentNode.getArray();
                            Node node = new Node(mValueContainer);
                            node.asStringValueNode(string);
                            node.parent = array;
                            array.addNode(node);
                        }

                        lastToken = TOKEN_VALUE;
                        break;

                    case BOOLEAN:

                        boolean booleanValue = jsonReader.nextBoolean();

                        if (lastToken == TOKEN_NAME) {

                            mCurrentNode.asBooleanValueNode(booleanValue);

                        } else if (lastToken == TOKEN_BEGIN_ARRAY || lastToken == TOKEN_VALUE) {

                            /* 上一步操作是BEGIN_ARRAY/TOKEN_VALUE ,说明当前节点是 json array; 并且它的元素都是值类型的 */
                            ArrayNodeTree array = mCurrentNode.getArray();
                            Node node = new Node(mValueContainer);
                            node.asBooleanValueNode(booleanValue);
                            node.parent = array;
                            array.addNode(node);
                        }

                        lastToken = TOKEN_VALUE;
                        break;

                    case NUMBER:

                        if (lastToken == TOKEN_NAME) {

                            mCurrentNode.asNumberValueNode(mNumberType, jsonReader);

                        } else if (lastToken == TOKEN_BEGIN_ARRAY || lastToken == TOKEN_VALUE) {

                            /* 上一步操作是BEGIN_ARRAY/TOKEN_VALUE ,说明当前节点是 json array; 并且它的元素都是值类型的 */

                            ArrayNodeTree array = mCurrentNode.getArray();
                            Node node = new Node(mValueContainer);
                            node.asNumberValueNode(mNumberType, jsonReader);
                            node.parent = array;
                            array.addNode(node);

                        }

                        lastToken = TOKEN_VALUE;
                        break;

                    case END_ARRAY:

                        jsonReader.endArray();

                        if (lastToken == TOKEN_END_OBJECT) {

                            NodeTree parent = mCurrentNode.parent;
                            mCurrentNode = parent.nodeLinkedToTree();
                        }

                        lastToken = TOKEN_END_ARRAY;
                        break;

                    case END_OBJECT:

                        jsonReader.endObject();

                        /* 主要工作:结束当前的 json object 节点添加,当前的节点还是指向object的子节点,将他移动到指向json object */

                        NodeTree parent = mCurrentNode.parent;
                        mCurrentNode = parent.nodeLinkedToTree();

                        lastToken = TOKEN_END_OBJECT;
                        break;

                    default:
                        break;
                }

                peek = jsonReader.peek();
            }

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {
                jsonReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return mCurrentNode.getObject();
    }


    private String lastTokenText(int lastToken) {

        String result = null;

        switch (lastToken) {
            case TOKEN_BEGIN_OBJECT:
                result = "BEGIN_OBJECT";
                break;

            case TOKEN_END_OBJECT:
                result = "END_OBJECT";
                break;

            case TOKEN_BEGIN_ARRAY:
                result = "BEGIN_ARRAY";
                break;

            case TOKEN_END_ARRAY:
                result = "END_ARRAY";
                break;

            case TOKEN_NAME:
                result = "NAME";
                break;

            case TOKEN_VALUE:
                result = "VALUE";
                break;

            default:
                result = "NO_TOKEN";
                break;
        }

        return result;
    }
}
