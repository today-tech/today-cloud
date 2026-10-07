/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
