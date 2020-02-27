
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
import sys

driverFoundID = 'e1vwiKmOzrfyDz34mHxjCri0aO73'
customerId = 'lnUSwIpKdoOwLdW4G8AAorPDyqk1'
rideId = 'lnUSwIpKdoOwLdW4G8AAorPDyqk112'

cred = credentials.Certificate("serviceAccountKey.json")
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://adhoc-aaffe.firebaseio.com'
})
if len(sys.argv[1:]) < 1:
    print("Usage : python3 server.py start/end")
    exit(1)
state = str(sys.argv[1])
if state == 'start':
    custreques = db.reference('/customerRequest')
    cust = custreques.child(customerId).child(rideId)
    cust_ht = {
        "g" : "tepbbzsx3d",
        "l" : [ 17.4448019, 78.3497604 ]
    }
    cust.update(cust_ht)

    ref = db.reference('/Users')
    customerRideId = ref.child("Drivers").child(driverFoundID).child("customerRideId")
    ht = {
        "customerId" : customerId,
        "rideId" : rideId
    }
    customerRideId.update(ht)

    print("CustomerRequest Initiated")
else:
    custreques = db.reference('/customerRequest')
    cust = custreques.child(customerId).child(rideId)
    cust.delete()

    ref = db.reference('/Users')
    customerRideId = ref.child("Drivers").child(driverFoundID).child("customerRideId")
    customerRideId.delete()

    print("CustomerRequest Deleted")
