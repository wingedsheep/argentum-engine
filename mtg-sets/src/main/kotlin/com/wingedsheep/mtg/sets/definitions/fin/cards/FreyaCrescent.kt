package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Freya Crescent
 * {R}
 * Legendary Creature — Rat Knight
 * 1/1
 *
 * Jump — During your turn, Freya Crescent has flying.
 * {T}: Add {R}. Spend this mana only to cast an Equipment spell or activate an equip ability.
 *
 * "Jump" is an ability word (no rules meaning); it's modeled as a conditional static grant of
 * flying to this creature gated on [Conditions.IsYourTurn] — the same shape as Dragoon's Lance.
 *
 * The mana restriction uses [ManaRestriction.SubtypeSpellsOrAbilitiesOnly] for the Equipment
 * subtype. This faithfully covers "cast an Equipment spell" and "activate an equip ability"
 * (equip is an activated ability of an Equipment source). The only over-broadness versus the
 * printed wording is that it would also permit paying for a *non-equip* activated ability of an
 * Equipment source — a marginal case (equip is almost always an Equipment's only activated
 * ability). A dedicated equip-only restriction would require threading an `isEquipAction` flag
 * through the activated-ability legal-action enumerator, which is out of scope for this card.
 */
val FreyaCrescent = card("Freya Crescent") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Rat Knight"
    oracleText = "Jump — During your turn, Freya Crescent has flying.\n" +
        "{T}: Add {R}. Spend this mana only to cast an Equipment spell or activate an equip ability."
    power = 1
    toughness = 1

    // Jump — During your turn, Freya Crescent has flying.
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(Keyword.FLYING, Filters.Self)
    }

    // {T}: Add {R}. Spend this mana only to cast an Equipment spell or activate an equip ability.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            color = Color.RED,
            amount = 1,
            restriction = ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Equipment")
        )
        manaAbility = true
        description = "{T}: Add {R}. Spend this mana only to cast an Equipment spell or activate an equip ability."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Nereida"
        flavorText = "\"We live not to forget our past, but to learn from it!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9921f646-e893-44db-ac89-0633c1009788.jpg?1748706279"
    }
}
