package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gideon Blackblade
 * {1}{W}{W}
 * Legendary Planeswalker — Gideon
 * Loyalty 4
 *
 * During your turn, Gideon Blackblade is a 4/4 Human Soldier creature with indestructible that's
 * still a planeswalker.
 * Prevent all damage that would be dealt to Gideon Blackblade during your turn.
 * +1: Up to one other target creature you control gains your choice of vigilance, lifelink, or
 *     indestructible until end of turn.
 * −6: Exile target nonland permanent.
 *
 * The animation is a set of your-turn conditional statics on Gideon himself that *add* the
 * creature type and the Human/Soldier subtypes — unlike Kaito, Bane of Nightmares, nothing removes
 * PLANESWALKER, so he keeps the Gideon planeswalker subtype and his loyalty. The damage shield is
 * a [PreventDamage] replacement scoped to Gideon and gated to your turn (the Frodo, Determined
 * Hero shape). The +1's keyword is chosen on resolution (ruling), so it's an
 * [Effects.ChooseAction] over the up-to-one target.
 */
val GideonBlackblade = card("Gideon Blackblade") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Gideon"
    startingLoyalty = 4
    oracleText = "During your turn, Gideon Blackblade is a 4/4 Human Soldier creature with " +
        "indestructible that's still a planeswalker.\n" +
        "Prevent all damage that would be dealt to Gideon Blackblade during your turn.\n" +
        "+1: Up to one other target creature you control gains your choice of vigilance, lifelink, " +
        "or indestructible until end of turn.\n" +
        "−6: Exile target nonland permanent."

    // "During your turn, Gideon Blackblade is a 4/4 Human Soldier creature with indestructible
    // that's still a planeswalker."
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantSubtype("Human", GroupFilter.source())
    }
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantSubtype("Soldier", GroupFilter.source())
    }
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = SetBasePowerToughnessStatic(4, 4, GroupFilter.source())
    }
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source())
    }

    // "Prevent all damage that would be dealt to Gideon Blackblade during your turn."
    replacementEffect(
        PreventDamage(
            restrictions = listOf(IsYourTurn),
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.Self)
        )
    )

    // +1: Up to one other target creature you control gains your choice of vigilance, lifelink,
    //     or indestructible until end of turn.
    loyaltyAbility(+1) {
        val creature = target(TargetFilter.OtherCreatureYouControl, optional = true)
        fun branch(label: String, keyword: Keyword) = EffectChoice(
            label = label,
            effect = Effects.GrantKeyword(keyword, creature),
        )
        effect = Effects.ChooseAction(
            listOf(
                branch("Vigilance until end of turn", Keyword.VIGILANCE),
                branch("Lifelink until end of turn", Keyword.LIFELINK),
                branch("Indestructible until end of turn", Keyword.INDESTRUCTIBLE),
            )
        )
    }

    // −6: Exile target nonland permanent.
    loyaltyAbility(-6) {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.Exile(permanent)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "13"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39d51c5e-26cb-4276-a676-fabfef866248.jpg?1783933482"
        ruling("2019-05-03", "If an effect causes Gideon to lose all abilities during your turn, he's still a Human Soldier creature and a Gideon planeswalker. This is because his first ability applies before that ability is lost.")
        ruling("2019-05-03", "If an Equipment becomes attached to Gideon while he's a creature, it'll become unattached during the next upkeep that isn't yours. The same is true of any Auras that become attached to Gideon that can't enchant a noncreature planeswalker.")
        ruling("2019-05-03", "Any counters that are put on Gideon remain on him while he's not a creature, even if they have no effect on a noncreature planeswalker.")
        ruling("2019-05-03", "If damage that can't be prevented is dealt to Gideon during your turn, that damage will have all applicable results: specifically, the damage is marked on Gideon (since he's a creature) and that damage causes that many loyalty counters to be removed from him (since he's a planeswalker). Even though he has indestructible, if Gideon has no loyalty counters on him, he's put into his owner's graveyard.")
        ruling("2019-05-03", "You choose which ability the target creature gains as Gideon's first loyalty ability resolves, not as you activate it.")
    }
}
