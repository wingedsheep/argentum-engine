package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Archdruid's Charm — Murders at Karlov Manor #151
 * {G}{G}{G} · Instant · Rare
 *
 * Choose one —
 * • Search your library for a creature or land card and reveal it. Put it onto the battlefield
 *   tapped if it's a land card. Otherwise, put it into your hand. Then shuffle.
 * • Put a +1/+1 counter on target creature you control. It deals damage equal to its power to
 *   target creature you don't control.
 * • Exile target artifact or enchantment.
 *
 * Three modes, three existing rails — nothing here is new vocabulary.
 *
 * **Mode 1** is the only one that needed thought. The search is a single search with a *branching
 * destination*, so it can't go through `Patterns.Library.searchLibrary` (one `SearchDestination`
 * for the whole call). Instead it is an inline [Effects.Pipeline]: gather creature-or-land from the
 * library, choose up to one, reveal, then `filterSplit` on [GameObjectFilter.Land] and move each
 * half to its own zone. The split is the load-bearing detail — "if it's a **land** card" is not
 * "if it isn't a creature card", so a land creature (Dryad Arbor, Ancient Tomb-style animations are
 * irrelevant here since this reads the *card*) goes to the battlefield, not to hand. Filtering on
 * `Creature` for the second half would double-move it.
 *
 * Both halves are `ChooseUpTo(1)`, so declining the search — or finding nothing — leaves both slots
 * empty and every downstream step is a no-op; the shuffle and the CR 701.23 "a player searched
 * their library" event still fire, because searching happened whether or not a card was found
 * (CR 701.23b). The reveal is `revealToSelf = false`: the searcher just picked the card out of
 * their own library, so only the opponents need to be shown it.
 *
 * **Mode 2** is [BiteDownOnCrime]'s shape exactly: two targets in one mode, and the damage reads
 * `EntityProperty(ContextTarget(0), Power)` with `damageSource = yours`, so the +1/+1 counter placed by
 * the first half is already on the creature when the power is read. Per the printed rulings, if the
 * opposing creature has become an illegal target by resolution the counter is still placed; if
 * *your* creature is gone, neither half happens. Both fall out of ordinary per-target legality
 * checking — the counter effect and the damage effect each fizzle on their own target.
 *
 * **Mode 3** is a plain [Effects.Exile] over [Targets.ArtifactOrEnchantment].
 */
val ArchdruidsCharm = card("Archdruid's Charm") {
    manaCost = "{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Search your library for a creature or land card and reveal it. Put it onto the " +
        "battlefield tapped if it's a land card. Otherwise, put it into your hand. Then shuffle.\n" +
        "• Put a +1/+1 counter on target creature you control. It deals damage equal to its power " +
        "to target creature you don't control.\n" +
        "• Exile target artifact or enchantment."

    spell {
        modal(chooseCount = 1) {
            mode(
                "Search your library for a creature or land card and reveal it. Put it onto the " +
                    "battlefield tapped if it's a land card. Otherwise, put it into your hand. " +
                    "Then shuffle."
            ) {
                effect = Effects.Pipeline {
                    val searchable = gather(
                        CardSource.FromZone(
                            Zone.LIBRARY,
                            Player.You,
                            GameObjectFilter.Creature or GameObjectFilter.Land,
                        ),
                        search = true
                    )
                    val found = chooseUpTo(
                        1,
                        from = searchable,
                        prompt = "Search your library for a creature or land card",
                    )
                    reveal(found, revealToSelf = false)
                    val (lands, others) = filterSplit(found, GameObjectFilter.Land)
                    move(
                        lands,
                        CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                    )
                    toHand(others)
                    run(Effects.ShuffleLibrary())
                    run(EmitLibrarySearchedEventEffect)
                }
            }

            mode(
                "Put a +1/+1 counter on target creature you control. It deals damage equal to its " +
                    "power to target creature you don't control."
            ) {
                val yours = target(TargetFilter.Creature.youControl())
                val theirs = target(TargetFilter.Creature.opponentControls())
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, yours) then
                    Effects.DealDamage(
                        amount = DynamicAmounts.powerOf(yours),
                        target = theirs,
                        damageSource = yours,
                    )
            }

            mode("Exile target artifact or enchantment") {
                val permanent = target(TargetFilter.ArtifactOrEnchantment)
                effect = Effects.Exile(permanent)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Liiga Smilshkalne"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5caae5ae-845f-42c2-b1ae-956df2739433.jpg?1783912870"

        ruling(
            "2024-02-02",
            "You can't choose the second mode for Archdruid's Charm unless you and another player " +
                "have creatures to target."
        )
        ruling(
            "2024-02-02",
            "If you choose the second mode and your creature is not a legal target as Archdruid's " +
                "Charm resolves, you won't put a +1/+1 counter on it, and no damage will be dealt."
        )
        ruling(
            "2024-02-02",
            "If you choose the second mode and the creature you don't control is not a legal " +
                "target as Archdruid's Charm resolves, you'll still put a +1/+1 counter on your " +
                "creature."
        )
    }
}
