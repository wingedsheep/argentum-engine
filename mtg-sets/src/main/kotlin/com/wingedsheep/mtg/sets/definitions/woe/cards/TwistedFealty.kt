package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Twisted Fealty
 * {2}{R}
 * Sorcery
 *
 * Gain control of target creature until end of turn. Untap that creature. It gains haste until
 * end of turn.
 * Create a Wicked Role token attached to up to one target creature.
 *
 * Two independent targets. The first is the borrowed creature and is required; the second is the
 * Role recipient and is "up to one" ([TargetCreature] with `optional = true`), so the spell is
 * castable with no second target and — per the 2023-09-01 ruling — simply skips the Role when the
 * second target is absent or has become illegal. Both are chosen at cast time, and the Role
 * recipient is any creature, not just the stolen one, so the two clauses stay separate effects
 * rather than one composed "steal and enchant".
 *
 * Effect order follows the printed order (gain control, then untap, then haste) rather than
 * Threaten's untap-first wording — with the control change first, the untap and the haste grant
 * both land on a creature already under your control.
 */
val TwistedFealty = card("Twisted Fealty") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gains " +
        "haste until end of turn.\n" +
        "Create a Wicked Role token attached to up to one target creature. (If you control another " +
        "Role on it, put that one into the graveyard. Enchanted creature gets +1/+1. When this " +
        "token is put into a graveyard, each opponent loses 1 life.)"

    spell {
        val stolen = target("target creature", Targets.Creature)
        val roleHost = target("up to one target creature", TargetCreature(optional = true))
        effect = Effects.Composite(
            Effects.GainControl(stolen, Duration.EndOfTurn),
            Effects.Untap(stolen),
            Effects.GrantKeyword(Keyword.HASTE, stolen),
            Effects.CreateRoleToken("Wicked Role", roleHost)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/382d6085-79b9-48f7-8949-9f44dde2c753.jpg?1783915087"

        ruling(
            "2023-09-01",
            "If you don't choose a second target for Twisted Fealty or that target is illegal as " +
                "the spell resolves, the Wicked Role token won't be created."
        )
        ruling(
            "2023-09-01",
            "Roles are colorless enchantment tokens. Each one has the Aura and Role subtypes and " +
                "the enchant creature ability."
        )
        ruling(
            "2023-09-01",
            "If a permanent has more than one Role attached to it controlled by the same player, " +
                "each of those Roles except the one with the most recent timestamp is put into its " +
                "owner's graveyard. This is a state-based action."
        )
        ruling(
            "2023-09-01",
            "A permanent can have multiple Roles attached to it if each one is controlled by a " +
                "different player."
        )
    }
}
