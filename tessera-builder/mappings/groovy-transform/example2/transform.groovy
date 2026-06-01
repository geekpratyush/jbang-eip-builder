import groovy.xml.XmlParser
import groovy.json.JsonOutput

def root = new XmlParser().parseText(body)

def doctors = root.doctors.doctor.collect { doc ->
    [
        id: doc.@id,
        name: doc.name.text(),
        specialty: doc.specialty.text()
    ]
}

def patients = root.patients.patient.collect { pat ->
    [
        id: pat.@id,
        status: pat.@status,
        name: pat.name.text(),
        age: pat.age.text() as Integer,
        assignedDoctorId: pat.assignedDoctor.text()
    ]
}

def result = [
    clinic: root.@name,
    city: root.@location,
    staffCount: doctors.size(),
    patientCount: patients.size(),
    medicalStaff: doctors,
    admittedPatients: patients
]

return JsonOutput.prettyPrint(JsonOutput.toJson(result))
