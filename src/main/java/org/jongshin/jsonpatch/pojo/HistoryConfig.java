package org.jongshin.jsonpatch.pojo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

/**
 * 
 * @author Vitalii_Kim
 *
 */
public class HistoryConfig implements Serializable {
	private static final Logger LOGGER = Logger.getLogger(HistoryConfig.class);
	private static final long serialVersionUID = -5324410355329301026L;

	private Map<String, List<EntityMapping>> mappings;

	public Map<String, List<EntityMapping>> getMappings() {
		return mappings;
	}

	public void setMappings(Map<String, List<EntityMapping>> mappings) {
		this.mappings = mappings;
	}

	@Override
	public String toString() {
		return "DfuHistoryConfig [mappings=" + mappings + "]";
	}

	public String getUid(String entityType, List<Object> path) {
		Preconditions.checkNotNull(entityType);
		Preconditions.checkNotNull(path);
		List<EntityMapping> entityMappings = mappings.get(entityType);
		if (entityMappings != null) {
			return entityMappings.stream()
					.filter(entityMapping -> entityMapping.getPath()
							.equals(path.stream().map(String::valueOf).collect(Collectors.joining())))
					.map(EntityMapping::getUid).findFirst().orElse(null);
		}
		LOGGER.error(String.format("Can't find entity mapping%npath: %s%nentityType: %s", path, entityType));
		return null;
	}

	private static class EntityMapping implements Serializable {

		private static final long serialVersionUID = 7504015184863472444L;

		private String path;
		private String uid;

		public String getPath() {
			return path;
		}

		public String getUid() {
			return uid;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			result = prime * result + ((uid == null) ? 0 : uid.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EntityMapping other = (EntityMapping) obj;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			if (uid == null) {
				if (other.uid != null)
					return false;
			} else if (!uid.equals(other.uid))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "EntityMapping [path=" + path + ", uid=" + uid + "]";
		}
	}
}
