package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

// Modern Oracle wording explicitly removes the creature from combat.
val Disharmony = card("Disharmony") {
    manaCost = "{2}{R}"
    typeLine = "Instant"
    oracleText = "Cast this spell only during combat before blockers are declared.\nUntap target attacking creature and remove it from combat. Gain control of that creature until end of turn."
    spell {
        castOnlyIf(Conditions.IsInStep(Step.BEGIN_COMBAT, Step.DECLARE_ATTACKERS, yoursOnly = false))
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.Untap(creature) then
            Effects.RemoveFromCombat(creature) then
            Effects.GainControl(creature, Duration.EndOfTurn)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Bryon Wackwitz"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e09a6b9b-eb0e-475d-8558-08e347412790.jpg?1783948057"
        ruling("2013-09-20", "If a turn has multiple combat phases, this spell can be cast during any of them as long as it's before the beginning of that phase's Declare Blockers Step.")
        ruling("2009-10-01", "If you cast Disharmony during an opponent's turn, you may then block with the creature you gain control of (assuming there's an attacking creature it can block).")
        ruling("2008-10-01", "Disharmony can target a creature you control.")
        ruling("2008-10-01", "The current wording explicitly removes the creature from combat. It is no longer an attacking or blocking creature.")
        ruling("2004-10-04", "Disharmony works even if the attacker was not tapped to attack.")
        ruling("2004-10-04", "The creature untaps (if it was tapped) before you get control of it.")
    }
}
