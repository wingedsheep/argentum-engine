package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Aurelia's Vindicator — Murders at Karlov Manor #4
 * {2}{W}{W} · Creature — Angel · Mythic
 * 4/2
 *
 * Flying, lifelink, ward {2}
 * Disguise {X}{3}{W}
 * When this creature is turned face up, exile up to X other target creatures from the battlefield
 * and/or creature cards from graveyards.
 * When this creature leaves the battlefield, return the exiled cards to their owners' hands.
 *
 * The printed `ward {2}` is a face-*up* keyword and is unrelated to the ward {2} every disguised
 * permanent has while face down (CR 702.168a) — that one rides `FaceDownMode.DISGUISE`. Both exist;
 * only one applies at a time.
 *
 * **X survives the flip already.** The disguise cost is the only place X is chosen, and the turn-up
 * is a special action, not a cast — so this is `DynamicAmount.XValue` (the ability's own X, carried
 * `TurnFaceUp.xValue` → `TurnFaceUpEvent.xValue` → `TriggerContext.xValue` → the trigger's stack
 * object), **not** `DynamicAmount.CastX`, which is the X paid to cast a *spell* and would read 0
 * here: this card was cast face down for {3}, with no X anywhere in that cost.
 *
 * The target is one requirement with a cross-zone union (CR 115.1) — `TargetFilter.OtherCreature`
 * or `TargetFilter.CreatureInGraveyard` — clamped by `dynamicMaxCount = XValue`, the same shape
 * Savior of Ollenbock uses one target at a time. `optional = true` is what makes X = 0, and "fewer
 * legal targets than X", resolve instead of fizzling.
 *
 * The exile is **not** [Effects.ExileUntilLeaves], and that is deliberate: that executor carries a
 * modern-template gate that skips the exile when the source has already left the battlefield, and
 * the printed ruling says the opposite — if the Vindicator dies with the trigger on the stack, the
 * leaves trigger does nothing and the turn-up trigger still exiles the targets, permanently. So the
 * effect gathers the chosen targets and moves them itself, linking them to the source; the link is
 * simply never read when the source is already gone. Gathering also handles all X targets at once,
 * where `ExileUntilLeaves` resolves a single [com.wingedsheep.sdk.scripting.targets.EffectTarget].
 *
 * The leaves trigger returns the whole linked pile to *hand*. The pile prunes itself — anything
 * that already left exile is dropped from `LinkedExileComponent` — so cards moved on by some other
 * effect aren't clawed back.
 */
val AureliasVindicator = card("Aurelia's Vindicator") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 4
    toughness = 2
    oracleText = "Flying, lifelink, ward {2}\n" +
        "Disguise {X}{3}{W} (You may cast this card face down for {3} as a 2/2 creature with " +
        "ward {2}. Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, exile up to X other target creatures from the " +
        "battlefield and/or creature cards from graveyards.\n" +
        "When this creature leaves the battlefield, return the exiled cards to their owners' hands."

    keywords(Keyword.FLYING, Keyword.LIFELINK)
    keywordAbility(KeywordAbility.ward("{2}"))
    disguise = "{X}{3}{W}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        target(
            "up to X other target creatures from the battlefield and/or creature cards from graveyards",
            TargetObject(
                optional = true,
                filter = TargetFilter.OtherCreature.or(TargetFilter.CreatureInGraveyard),
                dynamicMaxCount = DynamicAmount.XValue,
            ),
        )
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(source = CardSource.ChosenTargets, storeAs = "vindicated"),
                MoveCollectionEffect(
                    from = "vindicated",
                    destination = CardDestination.ToZone(Zone.EXILE),
                    linkToSource = true,
                ),
            )
        )
        description = "When this creature is turned face up, exile up to X other target creatures " +
            "from the battlefield and/or creature cards from graveyards."
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileToHand()
        description = "When this creature leaves the battlefield, return the exiled cards to " +
            "their owners' hands."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/5901dff4-e09b-4747-9297-797a1a057cd5.jpg?1783912930"

        ruling(
            "2024-02-02",
            "If Aurelia's Vindicator leaves the battlefield before its \"turned face up\" ability " +
                "has resolved, its leaves the battlefield ability will trigger and do nothing. Then " +
                "the \"turned face up\" ability will resolve and exile the targeted creatures and/or " +
                "creature cards indefinitely."
        )
        ruling(
            "2024-02-02",
            "If a creature targeted by the \"turned face up\" ability dies before that ability " +
                "resolves, it will become an illegal target even though it may be a creature card " +
                "in a graveyard when the ability resolves. It won't be exiled."
        )
        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
    }
}
