package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.CanBlockAnyNumber
import com.wingedsheep.sdk.scripting.MustBlockEachAttacker
import com.wingedsheep.sdk.scripting.Duration

val BlazeOfGlory = card("Blaze of Glory") {
    manaCost = "{W}"
    typeLine = "Instant"
    oracleText = "Cast this spell only during combat before blockers are declared.\nTarget creature defending player controls can block any number of creatures this turn. It blocks each attacking creature this turn if able."
    spell {
        castOnlyIf(Conditions.IsInStep(Step.BEGIN_COMBAT, Step.DECLARE_ATTACKERS, yoursOnly = false))
        val creature = target(TargetFilter.Creature.defendingPlayerControls())
        effect = Effects.GrantStaticAbility(CanBlockAnyNumber(), creature, Duration.EndOfTurn) then
            Effects.GrantStaticAbility(MustBlockEachAttacker(), creature, Duration.EndOfTurn)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Richard Thomas"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98fba951-c5bb-497c-9292-ce1b2a1e1247.jpg?1783948716"
        ruling("2013-09-20", "If a turn has multiple combat phases, this spell can be cast during any of them as long as it's before the beginning of that phase's Declare Blockers Step.")
        ruling("2004-10-04", "Does not allow a tapped creature to block, or allow a creature to block any creatures it would not normally be able to block.")
    }
}
