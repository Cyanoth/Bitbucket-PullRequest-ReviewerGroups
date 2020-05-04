# For testing purposes only. Just quickly thrown together...
# Purposely no http status code checks...
import requests
import random

API_URL = "http://localhost:7990/bitbucket/rest/api/1.0"  # Do not run this on production
ADMIN_USERNAME = "admin"
ADMIN_PASSWORD = "admin"  # Default for bitbucket debug env
DEFAULT_PROJECT = "PROJECT_1"
DEFAULT_REPO = "rep_1"
MAKE_USER_COUNT = 9 # Debug bitbucket license has a limit of 12 users. Default includes 2 already: 'admin' and 'user'.
MAKE_GROUP_COUNT = 10
CHANCE_USER_ADDED_TO_GROUP = 0.5

HEADERS = {'X-Atlassian-Token': 'no-check'}

MADE_GROUPS_GROUPNAMES=[]
MADE_USERS_USERNAMES=[]


def create_users():
    for i in range(MAKE_USER_COUNT):
        generated_user = "user%d" % i
        print("User: Creating user: %s" % generated_user)
        params = {
            "name": generated_user,
            "password": generated_user,
            "displayName": generated_user,
            "emailAddress": "%s@localhost" % generated_user
        }
        requests.post("%s/admin/users" % API_URL, auth=(ADMIN_USERNAME, ADMIN_PASSWORD), params=params, headers=HEADERS)
        MADE_USERS_USERNAMES.append(generated_user)


def create_groups():
    for i in range(MAKE_GROUP_COUNT):
        generate_group = "group%d" % i
        print("Group: Creating group: %s" % generate_group)
        params = {
            "name": generate_group
        }
        requests.post("%s/admin/groups" % API_URL, auth=(ADMIN_USERNAME, ADMIN_PASSWORD), params=params, headers=HEADERS)
        MADE_GROUPS_GROUPNAMES.append(generate_group)


def add_users_to_group():
    for group in MADE_GROUPS_GROUPNAMES:
        for user in MADE_USERS_USERNAMES:
            if random.choice([True, False]):
                print("Adding user: %s to group: %s" % (user, group))
                payload = {
                    "user": user,
                    "groups": [
                        group
                    ]
                }
                requests.post("%s/admin/users/add-groups" % API_URL, auth=(ADMIN_USERNAME, ADMIN_PASSWORD), json=payload, headers=HEADERS)

def add_groups_to_repo():
    for group in MADE_GROUPS_GROUPNAMES:
        print("Adding group: %s to: %s/%s" % (group, DEFAULT_PROJECT, DEFAULT_REPO))
        params = {
            "permission": "REPO_READ",
            "name": group
        }
        requests.put("%s/projects/%s/repos/%s/permissions/groups" % (API_URL, DEFAULT_PROJECT, DEFAULT_REPO),
                      auth=(ADMIN_USERNAME, ADMIN_PASSWORD), params=params, headers=HEADERS)


def main():
    create_users()
    create_groups()
    add_users_to_group()
    add_groups_to_repo()


if __name__ == "__main__":
    main()
