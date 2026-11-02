package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Aurelia, Exemplar of Justice
 * {2}{R}{W}
 * Legendary Creature — Angel
 * 2/5
 *
 * Flying
 * Mentor
 * At the beginning of combat on your turn, choose up to one target creature you control. Until end
 * of turn, that creature gets +2/+0, gains trample if it's red, and gains vigilance if it's white.
 *
 * The colour checks are made once, as the trigger resolves (`Effects.If` against the target), so the
 * granted keywords stay for the turn even if the creature's colours change afterwards (2018-10-05
 * ruling). The +2/+0 is applied unconditionally and only once.
 */
val AureliaExemplarOfJustice = card("Aurelia, Exemplar of Justice") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Angel"
    power = 2
    toughness = 5
    oracleText = "Flying\n" +
        "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature with " +
        "lesser power.)\n" +
        "At the beginning of combat on your turn, choose up to one target creature you control. Until end " +
        "of turn, that creature gets +2/+0, gains trample if it's red, and gains vigilance if it's white."

    keywords(Keyword.FLYING)
    mentor()

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val t = target(TargetFilter.CreatureYouControl, optional = true)
        effect = Effects.ModifyStats(2, 0, t) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.withColor(Color.RED), t),
                then = Effects.GrantKeyword(Keyword.TRAMPLE, t),
            ) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.withColor(Color.WHITE), t),
                then = Effects.GrantKeyword(Keyword.VIGILANCE, t),
            )
        description = "At the beginning of combat on your turn, choose up to one target creature you control. " +
            "Until end of turn, that creature gets +2/+0, gains trample if it's red, and gains vigilance if " +
            "it's white."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "153"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8e9f4d2-bba5-4061-8ae7-a68b912f2c11.jpg?1783934144"
        ruling(
            "2018-10-05",
            "Aurelia's last ability resolves before attackers are chosen."
        )
        ruling(
            "2018-10-05",
            "The target creature gains trample and vigilance if it's both red and white. It gets +2/+0 only " +
                "once, even if it's both red and white, and even if it's neither red nor white."
        )
        ruling(
            "2018-10-05",
            "Once Aurelia's last ability has resolved, the creature keeps whatever bonuses it got from that " +
                "ability even if its colors change."
        )
    }
}
