# Identity Infrastructure

OpenLDAP + Keycloak for local development. No external dependencies.

## Quick Start

```bash
cd infra
cp .env.example .env      # edit passwords if you want, defaults are fine for dev
docker compose up -d
```

Keycloak admin console: [http://localhost:8080](http://localhost:8080)

## LDAP Bind Credentials

| Bind DN | Password (default) | Purpose |
|---|---|---|
| `cn=admin,dc=sentinel,dc=local` | `admin` | Full read/write access |
| `cn=readonly,dc=sentinel,dc=local` | `readonly` | Read-only — use this for Keycloak LDAP federation |

## Seeded Users

| uid | cn | mail | Password | Groups |
|---|---|---|---|---|
| `shree.admin` | Shree Admin | shree.admin@sentinel.local | `password123` | finance-admins, employees |
| `rishabh.user` | Rishabh User | rishabh.user@sentinel.local | `password123` | employees |
| `aman.user` | Aman User | aman.user@sentinel.local | `password123` | employees |

## Seeded Groups

| Group CN | Members |
|---|---|
| `finance-admins` | shree.admin |
| `employees` | shree.admin, rishabh.user, aman.user |

## Verify LDAP

Search all entries with the admin bind:

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=sentinel,dc=local" \
  -w admin \
  -b "dc=sentinel,dc=local"
```

Or use the readonly bind (this is what Keycloak will use):

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=readonly,dc=sentinel,dc=local" \
  -w readonly \
  -b "dc=sentinel,dc=local"
```

## Tear Down

```bash
docker compose down           # stop containers, keep data volumes
docker compose down -v        # stop containers AND delete LDAP data (clean slate)
```

## LDAP Federation (Next Step)

Realm configuration is done manually through the Keycloak admin console.
Once LDAP federation is working, we will export the realm to `realm-export.json`
for reproducible setup.
