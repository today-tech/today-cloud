/*
 * Copyright 2021 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package infra.remoting.micrometer.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

enum RemotingObservationDocumentation implements ObservationDocumentation {

  /**
   * Observation created on the responder side.
   */
  RESPONDER {
    @Override
    public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
      return DefaultChannelResponderObservationConvention.class;
    }
  },

  /**
   * Observation created on the requester side for Fire and Forget frame type.
   */
  REQUESTER_FNF {
    @Override
    public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
      return DefaultChannelRequesterObservationConvention.class;
    }

    @Override
    public KeyName[] getLowCardinalityKeyNames() {
      return RequesterTags.values();
    }

    @Override
    public String getPrefix() {
      return "infra.remoting.";
    }
  },

  /** Observation created on the responder side for Fire and Forget frame type. */
  RESPONDER_FNF {
    @Override
    public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
      return DefaultChannelResponderObservationConvention.class;
    }

    @Override
    public KeyName[] getLowCardinalityKeyNames() {
      return ResponderTags.values();
    }

    @Override
    public String getPrefix() {
      return "infra.remoting.";
    }
  },

  /** Observation created on the requester side for Request Response frame type. */
  REQUESTER_REQUEST_RESPONSE {
    @Override
    public Class<? extends ObservationConvention<? extends Observation.Context>>
    getDefaultConvention() {
      return DefaultChannelRequesterObservationConvention.class;
    }

    @Override
    public KeyName[] getLowCardinalityKeyNames() {
      return RequesterTags.values();
    }

    @Override
    public String getPrefix() {
      return "infra.remoting.";
    }
  },

  /** Observation created on the responder side for Request Response frame type. */
  RESPONDER_REQUEST_RESPONSE {
    @Override
    public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
      return DefaultChannelResponderObservationConvention.class;
    }

    @Override
    public KeyName[] getLowCardinalityKeyNames() {
      return ResponderTags.values();
    }

    @Override
    public String getPrefix() {
      return "infra.remoting.";
    }
  },

  /** Observation created on the requester side for Request Stream frame type. */
  REQUESTER_REQUEST_STREAM {
    @Override
    public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
      return DefaultChannelRequesterObservationConvention.class;
    }

    @Override
    public KeyName[] getLowCardinalityKeyNames() {
      return RequesterTags.values();
    }

    @Override
    public String getPrefix() {
      return "infra.remoting.";
    }
  },

  /** Observation created on the responder side for Request Stream frame type. */
  RESPONDER_REQUEST_STREAM {
    @Override
    public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
      return DefaultChannelResponderObservationConvention.class;
    }

    @Override
    public KeyName[] getLowCardinalityKeyNames() {
      return ResponderTags.values();
    }

    @Override
    public String getPrefix() {
      return "infra.remoting.";
    }
  },

  /** Observation created on the requester side for Request Channel frame type. */
  REQUESTER_REQUEST_CHANNEL {
    @Override
    public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
      return DefaultChannelRequesterObservationConvention.class;
    }

    @Override
    public KeyName[] getLowCardinalityKeyNames() {
      return RequesterTags.values();
    }

    @Override
    public String getPrefix() {
      return "infra.remoting.";
    }
  },

  /** Observation created on the responder side for Request Channel frame type. */
  RESPONDER_REQUEST_CHANNEL {
    @Override
    public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
      return DefaultChannelResponderObservationConvention.class;
    }

    @Override
    public KeyName[] getLowCardinalityKeyNames() {
      return ResponderTags.values();
    }

    @Override
    public String getPrefix() {
      return "infra.remoting.";
    }
  };

  enum RequesterTags implements KeyName {

    /** Name of the route. */
    ROUTE {
      @Override
      public String asString() {
        return "infra.remoting.route";
      }
    },

    /** Name of the request type. */
    REQUEST_TYPE {
      @Override
      public String asString() {
        return "infra.remoting.request-type";
      }
    },

    /** Name of the content type. */
    CONTENT_TYPE {
      @Override
      public String asString() {
        return "infra.remoting.content-type";
      }
    }
  }

  enum ResponderTags implements KeyName {

    /** Name of the route. */
    ROUTE {
      @Override
      public String asString() {
        return "infra.remoting.route";
      }
    },

    /** Name of the request type. */
    REQUEST_TYPE {
      @Override
      public String asString() {
        return "infra.remoting.request-type";
      }
    }
  }
}
