package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Culvert Ambusher — Murders at Karlov Manor #158
 * {3}{G}{G} · Creature — Wurm Horror · 4/5
 *
 * When this creature enters or is turned face up, target creature blocks this turn if able.
 * Disguise {4}{G}
 *
 * Green's disguise ambush: cast it face down for {3}, attack with the 2/2, then flip for {4}{G} in
 * the declare-blockers step — except the flip trigger drags a creature *into* the block instead of
 * out of it, so the 4/5 eats whatever it names. Hard-cast for {3}{G}{G} the same trigger forces a
 * bad block on the defending turn.
 *
 * Two trigger conditions on one printed ability, so two `triggeredAbility` blocks (the
 * [PerimeterEnforcer] idiom). No event satisfies both: turning face up is not entering the
 * battlefield (CR 707.9a), and a card cast face down enters as a face-down 2/2 — its enters trigger
 * doesn't exist yet, because a face-down permanent has no abilities. So the face-down line really
 * does get exactly one trigger, on the flip.
 *
 * "Blocks this turn if able" is a *requirement*, not a guarantee (CR 509.1c) — hence
 * [Effects.MarkMustBlockThisTurn] rather than anything that reaches into block declaration. A
 * target that is tapped, that can't block, or whose every block would be illegal is excused, and
 * its controller never has to pay a cost associated with blocking. The target is chosen when the
 * trigger goes on the stack; if it's gone by resolution the trigger fizzles.
 *
 * Note it is plain `target creature` — the Ambusher may target its own controller's creature, and
 * the requirement is "block *something*", not "block the Ambusher".
 */
val CulvertAmbusher = card("Culvert Ambusher") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm Horror"
    power = 4
    toughness = 5
    oracleText = "When this creature enters or is turned face up, target creature blocks this turn " +
        "if able.\n" +
        "Disguise {4}{G} (You may cast this card face down for {3} as a 2/2 creature with ward " +
        "{2}. Turn it face up any time for its disguise cost.)"

    disguise = "{4}{G}"

    // When this creature enters …
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = Targets.Creature
        effect = Effects.MarkMustBlockThisTurn()
        description = "When this creature enters, target creature blocks this turn if able."
    }

    // … or is turned face up.
    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        target = Targets.Creature
        effect = Effects.MarkMustBlockThisTurn()
        description = "When this creature is turned face up, target creature blocks this turn if able."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Slawomir Maniak"
        flavorText = "The bite at the end of the tunnel."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2ccdc58b-1e7e-402c-88f9-c789ff1dae31.jpg?1783912868"

        ruling(
            "2024-02-02",
            "If the target creature is tapped or is affected by a spell or ability that says it " +
                "can't block, then it doesn't block. If there's a cost associated with having " +
                "that creature block, its controller isn't forced to pay that cost, so it doesn't " +
                "have to block in that case either."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
    }
}
