package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Rith, Liberated Primeval
 * {2}{R}{G}{W}
 * Legendary Creature — Dragon
 * 5/5
 * Flying, ward {2}
 * Other Dragons you control have ward {2}.
 * At the beginning of your end step, if a creature or planeswalker an opponent controlled was
 * dealt excess damage this turn, create a 4/4 red Dragon creature token with flying.
 *
 * The end-step condition is turn history keyed on the recipient's controller as the damage was
 * dealt: it holds even if that creature died or changed control afterwards, and whoever's source
 * dealt the damage (Scryfall rulings, 2022-09-09).
 */
val RithLiberatedPrimeval = card("Rith, Liberated Primeval") {
    manaCost = "{2}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Dragon"
    oracleText = "Flying, ward {2}\nOther Dragons you control have ward {2}.\n" +
        "At the beginning of your end step, if a creature or planeswalker an opponent controlled was " +
        "dealt excess damage this turn, create a 4/4 red Dragon creature token with flying."
    power = 5
    toughness = 5

    keywords(Keyword.FLYING, Keyword.WARD)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    staticAbility {
        ability = GrantWard(
            cost = WardCost.Mana("{2}"),
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.DRAGON).youControl()).other()
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.OpponentPermanentWasDealtExcessDamageThisTurn
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "214"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac148784-b7b6-4aae-bffb-a4cf09f56971.jpg?1783921278"
        ruling(
            "2022-09-09",
            "A creature has been dealt excess damage if one or more sources dealt more damage to it than the " +
                "minimum amount of damage required to be lethal damage. In most cases, this means damage greater " +
                "than its toughness, but consider the damage already dealt to it that turn."
        )
        ruling("2022-09-09", "A planeswalker is dealt excess damage if it's dealt damage greater than its current loyalty.")
        ruling(
            "2022-09-09",
            "Even 1 damage dealt to a creature from a source with deathtouch is considered lethal damage, so any " +
                "amount greater than that will cause excess damage to be dealt, even if the total amount of damage " +
                "isn't greater than the creature's toughness. Note that a source of damage having deathtouch has no " +
                "effect on damage dealt to planeswalkers."
        )
        ruling(
            "2022-09-09",
            "It doesn't matter whether a creature, spell, or ability you control deals the excess damage, only " +
                "that excess damage was dealt at some point during the turn."
        )
        ruling(
            "2022-09-09",
            "Rith's ability will trigger as long as an opponent controlled the creature as excess damage was dealt " +
                "to it, even if the creature is no longer on the battlefield or you control it at the end of the turn."
        )
    }
}
