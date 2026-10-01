package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slobad, Iron Goblin
 * {2}{R}
 * Legendary Creature — Phyrexian Goblin Artificer
 * 3/3
 * {T}, Sacrifice an artifact: Add an amount of {R} equal to the sacrificed artifact's mana value.
 * Spend this mana only to cast artifact spells or activate abilities of artifacts.
 *
 * Priest of Yawgmoth's shape (the sacrificed artifact's last-known mana value via
 * [EffectTarget.SacrificedAsCost]) plus the artifact spend restriction with both halves allowed.
 */
val SlobadIronGoblin = card("Slobad, Iron Goblin") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Phyrexian Goblin Artificer"
    power = 3
    toughness = 3
    oracleText = "{T}, Sacrifice an artifact: Add an amount of {R} equal to the sacrificed artifact's mana value. " +
        "Spend this mana only to cast artifact spells or activate abilities of artifacts."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.Artifact))
        effect = Effects.AddMana(
            Color.RED,
            amount = DynamicAmounts.manaValueOf(EffectTarget.SacrificedAsCost(0)),
            restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                cardType = CardType.ARTIFACT,
                allowSpells = true,
                allowAbilities = true,
            ),
        )
        manaAbility = true
        description = "{T}, Sacrifice an artifact: Add an amount of {R} equal to the sacrificed artifact's mana value. " +
            "Spend this mana only to cast artifact spells or activate abilities of artifacts."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "149"
        artist = "Chris Seaman"
        flavorText = "Once he fought to save Mirrodin. Now he fights to remake it."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b7dadf0-7323-403e-b353-33c121bdb0db.jpg?1783918024"
    }
}
