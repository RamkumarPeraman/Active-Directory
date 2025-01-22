#include <iostream>
#include <ldap.h>
#include <cstring>
#include <cstdlib>
#include "../ldap_config.h"

using namespace std;

LDAP* ld;
int rc;

void ldapBind() {
    rc = ldap_initialize(&ld, ldap_server);
    if (rc != LDAP_SUCCESS) {
        cerr << "Failed to initialize LDAP connection: " << ldap_err2string(rc) << endl;
        exit(EXIT_FAILURE);
    }
    int ldap_version = LDAP_VERSION3;
    ldap_set_option(ld, LDAP_OPT_PROTOCOL_VERSION, &ldap_version);
    BerValue cred;
    cred.bv_val = (char*)password;
    cred.bv_len = strlen(password);
    rc = ldap_sasl_bind_s(ld, username, LDAP_SASL_SIMPLE, &cred, NULL, NULL, NULL);
    if (rc != LDAP_SUCCESS) {
        cerr << "LDAP bind failed: " << ldap_err2string(rc) << endl;
        ldap_unbind_ext_s(ld, NULL, NULL);
        exit(EXIT_FAILURE);
    }
}

bool check_attribute_value(LDAP* ldap_handle, const char* dn, const char* attribute, const char* value) {
    LDAPMessage* result;
    LDAPMessage* entry;
    BerElement* ber;
    char* attribute_name;
    char** vals;

    char* attrs[] = {const_cast<char*>(attribute), NULL};
    rc = ldap_search_ext_s(ldap_handle, dn, LDAP_SCOPE_BASE, NULL, attrs, 0, NULL, NULL, NULL, 0, &result);
    if (rc != LDAP_SUCCESS) {
        cerr << "LDAP search failed: " << ldap_err2string(rc) << endl;
        return false;
    }

    entry = ldap_first_entry(ldap_handle, result);
    if (entry == NULL) {
        cerr << "Entry not found" << endl;
        ldap_msgfree(result);
        return false;
    }

    for (attribute_name = ldap_first_attribute(ldap_handle, entry, &ber);
         attribute_name != NULL;
         attribute_name = ldap_next_attribute(ldap_handle, entry, ber)) {
        if ((vals = ldap_get_values(ldap_handle, entry, attribute_name)) != NULL) {
            for (int i = 0; vals[i] != NULL; i++) {
                if (strcmp(vals[i], value) == 0) {
                    ldap_value_free(vals);
                    ldap_memfree(attribute_name);
                    ber_free(ber, 0);
                    ldap_msgfree(result);
                    return true;
                }
            }
            ldap_value_free(vals);
        }
        ldap_memfree(attribute_name);
    }
    ber_free(ber, 0);
    ldap_msgfree(result);
    return false;
}

void modify_attribute(LDAP* ldap_handle, const char* dn, const char* attribute, const char* new_value) {
    if (check_attribute_value(ldap_handle, dn, attribute, new_value)) {
        cout << "Value is already up to date." << endl;
        return;
    }

    LDAPMod mod;
    LDAPMod* mods[2];
    mod.mod_op = LDAP_MOD_REPLACE;
    mod.mod_type = const_cast<char*>(attribute);
    char* vals[2];
    vals[0] = const_cast<char*>(new_value);
    vals[1] = nullptr;
    mod.mod_vals.modv_strvals = vals;
    mods[0] = &mod;
    mods[1] = nullptr;
    int rc = ldap_modify_ext_s(ldap_handle, dn, mods, nullptr, nullptr);
    if (rc != LDAP_SUCCESS) {
        cerr << "ldap_modify_ext_s error: " << ldap_err2string(rc) << endl;
    } else {
        cout << "Attribute modified successfully." << endl;
    }
}

int main(int argc, char* argv[]) {
    if (argc != 4) {
        cerr << "Usage: " << argv[0] << " <Name> <attribute> <value>" << endl;
        return 1;
    }
    const char* cn = argv[1];
    const char* attribute = argv[2];
    const char* new_value = argv[3];
    string dn = string("CN=") + cn + ",CN=Users,DC=zoho,DC=com";
    ldapBind();
    modify_attribute(ld, dn.c_str(), attribute, new_value);
    ldap_unbind_ext_s(ld, NULL, NULL);

    return 0;
}