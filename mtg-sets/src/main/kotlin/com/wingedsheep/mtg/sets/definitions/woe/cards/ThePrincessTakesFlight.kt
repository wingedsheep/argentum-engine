package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * The Princess Takes Flight
 * {2}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Exile up to one target creature.
 * II — Target creature you control gets +2/+2 and gains flying until end of turn.
 * III — Return the exiled card to the battlefield under its owner's control.
 *
 * The blink is held together by the Saga's *linked exile*, not by a remembered entity id: chapter I
 * moves the creature to exile with `linkToSource = true`, and chapter III gathers back over
 * [CardSource.FromLinkedExile]. That is what makes the card behave correctly when the exiled
 * permanent is a token (it ceases to exist in exile, so chapter III finds nothing and returns
 * nothing) and when chapter I exiled several creatures because the ability was copied — the gather
 * picks up every linked card, which is exactly what the printed ruling calls for.
 *
 * Chapter I is "up to one target", so it is legal with no target at all and simply does nothing;
 * chapter III then has nothing to return. The return is `underOwnersControl = true` — exiling an
 * opponent's creature hands it back to them, it does not steal it.
 */
val ThePrincessTakesFlight = card("The Princess Takes Flight") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Exile up to one target creature.\n" +
        "II — Target creature you control gets +2/+2 and gains flying until end of turn.\n" +
        "III — Return the exiled card to the battlefield under its owner's control."

    sagaChapter(1) {
        val creature = target(
            "up to one target creature",
            TargetCreature(optional = true, filter = TargetFilter.Creature)
        )
        effect = Effects.Move(creature, Zone.EXILE, linkToSource = true)
    }

    sagaChapter(2) {
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.ModifyStats(2, 2, creature)
            .then(Effects.GrantKeyword(Keyword.FLYING, creature))
    }

    sagaChapter(3) {
        effect = Effects.Composite(
            GatherCardsEffect(source = CardSource.FromLinkedExile(), storeAs = "princessExiled"),
            MoveCollectionEffect(
                from = "princessExiled",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                underOwnersControl = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Julia Metzger"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/dad7bd06-22e4-40f8-bda9-bcdbb2d8f632.jpg?1783915128"

        ruling(
            "2023-09-01",
            "If The Princess Takes Flight's first chapter ability exiled more than one creature " +
                "(usually because the ability was copied), its third chapter ability will return " +
                "all of the exiled cards to the battlefield under their owners' control."
        )
    }
}
