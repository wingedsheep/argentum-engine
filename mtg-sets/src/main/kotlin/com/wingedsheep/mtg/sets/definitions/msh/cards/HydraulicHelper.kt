package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Hydraulic Helper — Marvel Super Heroes #57
 * {1}{U} · Artifact Creature — Robot · 2/3
 *
 * Defender
 * {T}: Add {U}. This mana can't be spent to cast a nonartifact spell.
 *
 * The printed restriction is **negative** — it forbids one spend rather than permitting one — so it
 * uses [ManaRestriction.CannotCastSpellsOtherThan] rather than a whitelist. The Purple Dragon Punks
 * composition (`AnyOf(CardTypeSpellsOrAbilitiesOnly(ARTIFACT), AbilityActivationOnly)`) is the
 * positive Guidelight Optimizer clause, "spend this mana only to cast an artifact spell or activate
 * an ability", and is strictly narrower: it also rejects paying a ward cost, an "unless that player
 * pays {2}" tax, or a cost demanded while something resolves — none of which is casting a spell.
 */
val HydraulicHelper = card("Hydraulic Helper") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Robot"
    power = 2
    toughness = 3
    oracleText = "Defender\n{T}: Add {U}. This mana can't be spent to cast a nonartifact spell."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.BLUE,
            1,
            restriction = ManaRestriction.CannotCastSpellsOtherThan(setOf(CardType.ARTIFACT)),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {U}. This mana can't be spent to cast a nonartifact spell."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Kevin Glint"
        flavorText = "\"Put it here. No, not there. I *said* not there. Great. You've ruined six million dollars' worth of research, and I still don't have my coffee.\"\n—Tony Stark"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06d8a2a0-775b-4813-973f-8b15612b38d1.jpg?1783902958"
    }
}
