package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.splice
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reweave
 * {5}{U}
 * Instant — Arcane
 * Target permanent's controller sacrifices it. If the player does, they reveal cards from the top
 * of their library until they reveal a permanent card that shares a card type with the sacrificed
 * permanent, put that card onto the battlefield, then shuffle.
 * Splice onto Arcane {2}{U}{U}
 *
 * Oracle errata (2008-04-01): the reveal looks for a *permanent* card that shares a card type,
 * so a kindred instant can't be found off a kindred enchantment — hence
 * `GameObjectFilter.Permanent` in front of the card-type share.
 *
 * The sacrifice is the target's *own controller's* (`sacrificedByItsController`), and "if the
 * player does" reads [DynamicAmounts.permanentsSacrificedThisWay] off that same step (the
 * Woebringer Demon shape), so a permanent that can't be sacrificed stops the whole rest of the
 * spell. The reveal is Polymorph's: gather until the first match in the target controller's
 * library (the controller is read through last-known information, since the permanent has
 * already left), reveal the pile, put the match onto the battlefield under that player's
 * control, then shuffle — the rest of the revealed cards never left the library.
 *
 * The card-type share reads the sacrificed card by its (kept) entity id in the graveyard, so it
 * compares its printed card types. Known deviation from the 2005-04-01 ruling ("uses the type(s)
 * the permanent had right before it was sacrificed"): a permanent whose card types were changed
 * by a continuous effect (an animated land, a creature made an artifact) is matched on its
 * printed types rather than its last-known ones.
 *
 * Splice: the spliced text resolves against its own target slice (CR 702.47), so the first-target
 * readings below ([Player.ControllerOf], [EffectTarget.TargetController]) name Reweave's target.
 */
val Reweave = card("Reweave") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Instant — Arcane"
    oracleText = "Target permanent's controller sacrifices it. If the player does, they reveal " +
        "cards from the top of their library until they reveal a permanent card that shares a " +
        "card type with the sacrificed permanent, put that card onto the battlefield, then " +
        "shuffle.\n" +
        "Splice onto Arcane {2}{U}{U} (As you cast an Arcane spell, you may reveal this card " +
        "from your hand and pay its splice cost. If you do, add this card's effects to that spell.)"

    splice("{2}{U}{U}")

    spell {
        val t = target(TargetFilter.Permanent)
        val thatPlayer = Player.ControllerOf("target permanent")
        effect = Effects.SacrificeTarget(t, sacrificedByItsController = true) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.permanentsSacrificedThisWay(),
                    ComparisonOperator.GTE,
                    1
                ),
                then = Effects.Pipeline {
                    val (found, allRevealed) = gatherUntilMatch(
                        GameObjectFilter.Permanent.sharingCardTypeWith(t),
                        player = thatPlayer
                    )
                    reveal(allRevealed, fromZone = Zone.LIBRARY, toZone = Zone.BATTLEFIELD)
                    move(found, CardDestination.ToZone(Zone.BATTLEFIELD, player = thatPlayer))
                    run(Effects.ShuffleLibrary(EffectTarget.TargetController))
                }
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Alex Horley-Orlandelli"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4ff68016-80b4-4801-b796-5c5fdbc8faa1.jpg?1783944322"
        ruling(
            "2008-04-01",
            "Reweave has received errata so that the effect looks for a permanent card that " +
                "shares a type with the sacrificed permanent. A permanent card is a card that " +
                "could be put onto the battlefield, which is any card that isn't an instant or sorcery."
        )
        ruling("2005-11-01", "The player reveals cards only if the targeted permanent gets sacrificed.")
        ruling(
            "2005-04-01",
            "When Reweave resolves it uses the type(s) that the permanent had right before it was sacrificed."
        )
    }
}
