#!/bin/bash -x

# ref: https://github.com/AKSW/hawk

#curl http://localhost:8181/search?q=What+is+the+capital+of+Germany+%3F will return a UUID.
curl http://localhost:8181/search?q=What+is+the+capital+of+Germany+%3F

#curl http://localhost:8181/status?UUID=00000000-0000-0000-0000-000000000001 gives you status updates
curl http://localhost:8181/status?UUID=00000000-0000-0000-0000-000000000001
