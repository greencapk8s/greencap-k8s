---
title: UserService — testes com @SpringBootTest + TestContainers (4 cenários)
status: done
sprint: 81
---

## Problem

`UserService` não tem cobertura de teste. Regressões nas regras de autenticação (usuário inativo consegue logar, senha armazenada em plaintext, permissões não persistem) ficam invisíveis.

## Expected

Nova classe `src/test/java/io/greencap/k8s/domain/user/UserServiceTest.java`.

### Abordagem

`@SpringBootTest` + `@Testcontainers` + `@ServiceConnection` — reutiliza o mesmo PostgreSQLContainer da issue 01. `UserService` é injetado via `@Autowired`. Nenhum mock necessário: todos os colaboradores (`UserRepository`, `PasswordEncoder`) são beans reais.

### Cenários

**1 — `loadUserByUsername` retorna `UserDetails` com authorities corretas**
- Criar usuário via `createUser` com `Set.of(Permission.GLOBAL_CLUSTERS_VIEW, Permission.WORKLOADS_PODS_VIEW)`
- Chamar `loadUserByUsername` e verificar que as authorities contêm exatamente `"GLOBAL_CLUSTERS_VIEW"` e `"WORKLOADS_PODS_VIEW"`

**2 — `loadUserByUsername` com usuário inativo lança `UsernameNotFoundException`**
- Criar usuário, chamar `deactivateUser`, depois tentar `loadUserByUsername`
- Verificar que lança `UsernameNotFoundException` — garantia de que usuários desativados não conseguem logar

**3 — `createUser` persiste senha como hash, nunca plaintext**
- Criar usuário com senha `"minhasenha"`
- Verificar que o hash armazenado **não é igual** a `"minhasenha"` (plaintext nunca no banco)
- Verificar que `passwordEncoder.matches("minhasenha", hash)` retorna `true`

**4 — `loadUserByUsername` com usuário inexistente lança `UsernameNotFoundException`**
- Chamar `loadUserByUsername("nao-existe")`
- Verificar que lança `UsernameNotFoundException`
