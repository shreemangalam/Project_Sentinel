# Identity Infrastructure

OpenLDAP + Keycloak for local development. No external dependencies.

## Quick Start

```bash
cd infra
cp .env.example .env      # edit passwords if you want, defaults are fine for dev
docker compose up -d
```

Keycloak admin console: [http://localhost:8080](http://localhost:8080)

The `Sentinel` realm is auto-imported on first startup from `realm-export.json`,
including LDAP federation config and group mapper. Users sync from LDAP
automatically — no manual console setup needed.

## LDAP Bind Credentials

| Bind DN | Password (default) | Purpose |
|---|---|---|
| `cn=admin,dc=sentinel,dc=local` | `admin` | Full read/write access, used by Keycloak federation |
| `cn=readonly,dc=sentinel,dc=local` | `readonly` | Read-only access |

## Seeded Users

| uid | cn | mail | Password | Groups |
|---|---|---|---|---|
| `shree.admin` | Shree Admin | shree.admin@sentinel.local | `password123` | finance-admins, employees |
| `rishabh.user` | Rishabh User | rishabh.user@sentinel.local | `password123` | hr-admins, employees |
| `aman.user` | Aman User | aman.user@sentinel.local | `password123` | employees |

## Seeded Groups

| Group CN | Members | Purpose |
|---|---|---|
| `finance-admins` | shree.admin | RBAC for finance-service endpoints |
| `hr-admins` | rishabh.user | RBAC for hr-service endpoints |
| `employees` | shree.admin, rishabh.user, aman.user | Base access group for all staff |

## Verify LDAP

Search all entries with the admin bind:

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=sentinel,dc=local" \
  -w admin \
  -b "dc=sentinel,dc=local"
```

## Tear Down

```bash
docker compose down           # stop containers, keep data volumes
docker compose down -v        # stop containers AND delete LDAP data (clean slate)
```

After `docker compose down -v`, the next `up` re-seeds LDAP from the LDIF
and re-imports the Keycloak realm from the JSON export.
