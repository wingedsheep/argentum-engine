package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Immolation Shaman — Ravnica Allegiance #106
 * {1}{R} · Creature — Lizard Shaman · 1 / 3
 *
 * Harsh Mentor's trigger at 1 damage. `.onBattlefield()` keeps cycling and graveyard abilities
 * out, per the ruling; the default activation gate already excludes mana abilities.
 */
val ImmolationShaman = card("Immolation Shaman") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Shaman"
    power = 1
    toughness = 3
    oracleText = "Whenever an opponent activates an ability of an artifact, creature, or land that isn't a mana ability, " +
        "this creature deals 1 damage to that player.\n" +
        "{3}{R}{R}: This creature gets +3/+3 and gains menace until end of turn."

    triggeredAbility {
        trigger = Triggers.anOpponent.activatesAbility(
            of = (GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Land).onBattlefield()
        )
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever an opponent activates an ability of an artifact, creature, or land that isn't a mana ability, " +
            "this creature deals 1 damage to that player."
    }

    activatedAbility {
        cost = Costs.Mana("{3}{R}{R}")
        effect = Effects.ModifyStats(3, 3, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self)
        description = "{3}{R}{R}: This creature gets +3/+3 and gains menace until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Svetlin Velinov"
        flavorText = "\"Hello, kindling.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45e668e5-ef50-43eb-852e-b111370459c8.jpg?1783933678"
        ruling(
            "2019-01-25",
            "Immolation Shaman's first ability doesn't trigger when an opponent activates an ability of a card in hand " +
                "(such as cycling) or a card in a graveyard (such as that of Bone Dragon), even if that causes a card to be put onto the battlefield."
        )
        ruling(
            "2019-01-25",
            "Activating Immolation Shaman's second ability after one creature has blocked it won't cause Immolation Shaman to become unblocked."
        )
    }
}
