package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Soul of Shandalar — Magic 2015 #163 (canonical printing)
 * {4}{R}{R} · Creature — Avatar · 6/6
 *
 * First strike
 * {3}{R}{R}: This creature deals 3 damage to target player or planeswalker and 3 damage to up to
 * one target creature that player or that planeswalker's controller controls.
 * {3}{R}{R}, Exile this card from your graveyard: It deals 3 damage to target player or
 * planeswalker and 3 damage to up to one target creature that player or that planeswalker's
 * controller controls.
 *
 * Soul of Theros's shape: both abilities share one effect; the second is activated from the
 * graveyard with exiling this card as part of its cost, and the exiled card is still the source of
 * the damage. The targeting is Chandra, Pyromaster's +1: a player-or-planeswalker target, then an
 * optional creature controlled by that player or by that planeswalker's controller.
 */
val SoulOfShandalar = card("Soul of Shandalar") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Avatar"
    power = 6
    toughness = 6
    oracleText = "First strike\n" +
        "{3}{R}{R}: This creature deals 3 damage to target player or planeswalker and 3 damage to up to " +
        "one target creature that player or that planeswalker's controller controls.\n" +
        "{3}{R}{R}, Exile this card from your graveyard: It deals 3 damage to target player or " +
        "planeswalker and 3 damage to up to one target creature that player or that planeswalker's " +
        "controller controls."

    keywords(Keyword.FIRST_STRIKE)

    activatedAbility {
        cost = Costs.Mana("{3}{R}{R}")
        val recipient = target(Targets.PlayerOrPlaneswalker)
        // "That player or that planeswalker's controller": the first branch reads a player
        // target, the second the controller of a planeswalker target.
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.targetPlayerControls(recipient) or
                    GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController),
            ),
            optional = true,
        )
        effect = Effects.DealDamage(3, recipient) then Effects.DealDamage(3, creature)
        description = "{3}{R}{R}: This creature deals 3 damage to target player or planeswalker and 3 " +
            "damage to up to one target creature that player or that planeswalker's controller controls."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{R}{R}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        val recipient = target(Targets.PlayerOrPlaneswalker)
        // "That player or that planeswalker's controller": the first branch reads a player
        // target, the second the controller of a planeswalker target.
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.targetPlayerControls(recipient) or
                    GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController),
            ),
            optional = true,
        )
        effect = Effects.DealDamage(3, recipient) then Effects.DealDamage(3, creature)
        description = "{3}{R}{R}, Exile this card from your graveyard: It deals 3 damage to target player or " +
            "planeswalker and 3 damage to up to one target creature that player or that planeswalker's " +
            "controller controls."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "163"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03543d10-202b-4b07-a34b-64216a745d84.jpg?1783939169"
        ruling("2014-07-18", "You can activate the last ability only if the Soul is in your graveyard.")
        ruling("2014-07-18", "Exiling the Soul from your graveyard is part of the last ability's activation cost. A player can't remove the Soul in response to prevent you from activating the ability.")
        ruling("2014-07-18", "To activate either ability, you must target a player, but you can choose to not target any creatures.")
    }
}
