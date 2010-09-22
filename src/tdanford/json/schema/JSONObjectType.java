package tdanford.json.schema;

import static java.lang.String.*;

import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;

import tdanford.json.schema.JSONType.AbstractType;

public class JSONObjectType extends AbstractType { 

	private SchemaEnv env;
	private java.lang.String name;
	private java.lang.String description;
	private Map<java.lang.String,JSONType> properties;

	public JSONObjectType(SchemaEnv env, JSONObject obj) throws SchemaException { 
		this.env = env;
		this.name = null;
		this.description = null;
		properties = new TreeMap<java.lang.String,JSONType>();
		
		//System.out.println(format("JSONObjecType parsing: \n%s", obj.toString()));
		
		try {
			if(!obj.has("type")) { 
				throw new SchemaException(
						format("Missing required 'type' property in \n%s", obj.toString()));
			}
			java.lang.String type = obj.getString("type").toLowerCase(); 
			if(!type.equals("object")) { 
				throw new SchemaException(java.lang.String.format(
						"Cannot convert expression %s into JSONObjectType", obj.toString()));
			}
			
			if(obj.has("name")) { 
				java.lang.String name = obj.getString("name");
				this.name = name;
				env.addType(name, this);
			}
			
			if(obj.has("description")) { 
				this.description = obj.getString("description");
			}
			
			if(obj.has("properties")) { 
				JSONObject propObj = obj.getJSONObject("properties"); 
				Iterator<java.lang.String> itr = (Iterator<java.lang.String>)propObj.keys();
				while(itr.hasNext()) { 
					java.lang.String propName = itr.next();
					properties.put(propName, env.evaluate(propObj.get(propName)));
				}
			}			
			
		} catch(JSONException e) { 
			throw new SchemaException(obj.toString(), e);
		}
	}
	
	public void putInEnv(SchemaEnv newEnv) { 
		if(name != null) { 
			newEnv.addType(name, this);
			this.env = newEnv; 
		} else { 
			throw new IllegalArgumentException("Cannot add a blank type to a SchemaEnv");
		}
	}
	
	public boolean contains(Object obj) { 
		if(obj == null || !(obj instanceof JSONObject)) { 
			return false; 
		}

		JSONObject json = (JSONObject)obj;
		Iterator keys = json.keys();
		Set<java.lang.String> toSee = new TreeSet<java.lang.String>(properties.keySet());
		Set<java.lang.String> seen = new TreeSet<java.lang.String>();

		while(keys.hasNext()) { 
			java.lang.String key = (java.lang.String)keys.next();
			if(seen.contains(key)) { 
				return false;
			}

			if(toSee.contains(key)) { 
				try { 
					if(!properties.get(key).contains(json.get(key))) { 
						return false;
					}
				} catch(JSONException e) { 
					throw new IllegalArgumentException(java.lang.String.format(
							"%s in %s", key, obj.toString()));
				}

				toSee.remove(key);
				seen.add(key);
			}
		}

		if(!toSee.isEmpty()) { 
			return false;
		}

		return true;
	}
	
	public java.lang.String explain(Object obj) {  
		if (obj == null) { 
			return format("REJECT: null value");
		} else { 
			JSONObject json = (JSONObject)obj;
			Iterator keys = json.keys();
			Set<java.lang.String> toSee = new TreeSet<java.lang.String>(properties.keySet());
			Set<java.lang.String> seen = new TreeSet<java.lang.String>();
			
			while(keys.hasNext()) { 
				java.lang.String key = (java.lang.String)keys.next();
				
				if(seen.contains(key)) { 
					return format("REJECT: unexpected duplicate key \"%s\" in %s", key, json.toString());
				}

				if(toSee.contains(key)) { 
					try { 
						if(!properties.get(key).contains(json.get(key))) { 
							return properties.get(key).explain(json.get(key));
						}
					} catch(JSONException e) { 
						throw new IllegalArgumentException(java.lang.String.format(
								"%s in %s", key, obj.toString()));
					}

					toSee.remove(key);
					seen.add(key);
				}
			}
			
			if(!toSee.isEmpty()) { 
				return format("REJECT: missing key(s): %s", toSee.toString());
			} 
			
		}
		return null;
	}

	public java.lang.String getName() {
		return name;
	}
	
	public java.lang.String getDescription() { 
		return description;
	}
	
	public java.lang.String[] getProperties() { 
		return properties.keySet().toArray(new java.lang.String[0]);
	}
	
	public JSONType getPropertyType(java.lang.String propName) { 
		return properties.get(propName);
	}
}
