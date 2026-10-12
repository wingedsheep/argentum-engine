package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantActivateAbilities
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Myrel, Shield of Argive — The Brothers' War #18 (canonical printing)
 * {3}{W} · Legendary Creature — Human Soldier · 3/4
 *
 * During your turn, your opponents can't cast spells or activate abilities of artifacts,
 * creatures, or enchantments.
 * Whenever Myrel attacks, create X 1/1 colorless Soldier artifact creature tokens, where X is
 * the number of Soldiers you control.
 *
 * The static half is Grand Abolisher's pair: [PlayersCantCastSpells] and
 * [PlayersCantActivateAbilities], both scoped to `EachOpponent` with `condition = IsYourTurn`.
 * `anyZone` stays false — per the ruling, only abilities of permanents on the battlefield are
 * stopped, so graveyard abilities such as unearth remain available.
 *
 * X is counted on resolution over "Soldiers" — the bare tribal noun, i.e. Soldier permanents you
 * control — so Myrel counts itself.
 */
val MyrelShieldOfArgive = card("Myrel, Shield of Argive") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 4
    oracleText = "During your turn, your opponents can't cast spells or activate abilities of " +
        "artifacts, creatures, or enchantments.\n" +
        "Whenever Myrel attacks, create X 1/1 colorless Soldier artifact creature tokens, where X " +
        "is the number of Soldiers you control."

    staticAbility {
        ability = PlayersCantCastSpells(affected = Player.EachOpponent, condition = IsYourTurn)
    }

    staticAbility {
        ability = PlayersCantActivateAbilities(
            affected = Player.EachOpponent,
            permanentFilter = GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Enchantment,
            condition = IsYourTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype("Soldier")).count(),
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Soldier"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/3/7/37da5b54-ec55-46e3-9f0b-565cbbe1ac7a.jpg?1783919907",
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "18"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e632ed57-bb00-4493-adef-7b4805edd7ea.jpg?1783920129"
        ruling(
            "2022-10-14",
            "Myrel, Shield of Argive's first ability prevents opponents from activating abilities of " +
                "permanents on the battlefield that are artifacts, creatures, and/or enchantments. It " +
                "doesn't prevent activated abilities from cards in any zone other than the battlefield. " +
                "Notably, this means that your opponents may still activate abilities of cards in their " +
                "graveyard (such as unearth)."
        )
    }
}
