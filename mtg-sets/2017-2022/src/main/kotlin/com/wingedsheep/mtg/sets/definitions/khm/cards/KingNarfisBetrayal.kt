package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * King Narfi's Betrayal
 * {1}{U}{B}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Each player mills four cards. Then you may exile a creature or planeswalker card from each
 *     graveyard.
 * II, III — Until end of turn, you may cast spells from among cards exiled with this Saga, and you
 *     may spend mana as though it were mana of any color to cast those spells.
 *
 * Chapter I mills every library, then walks each graveyard with the Saga's controller choosing
 * (`Chooser.SourceController` — the per-player loop rebinds "you" to the iterated player) up to one
 * creature or planeswalker card from it, freshly milled or already there (ruling). The picks are
 * exiled together and linked to this Saga (CR 607 "exiled with"), so only this Betrayal's cards
 * count. Chapters II and III gather that linked pile and grant an end-of-turn cast permission
 * ("cast", so never a land play) with any-colour spending; the grant outlives the Saga being
 * sacrificed after III. A card cast this way becomes a new object and leaves the pile (ruling).
 */
private val creatureOrPlaneswalker = GameObjectFilter.Creature or GameObjectFilter.Planeswalker

private val castFromBetrayalExile = Effects.Pipeline {
    val exiled = gather(CardSource.FromLinkedExile())
    run(Effects.GrantMayPlayFromExile(
        from = exiled,
        expiry = MayPlayExpiry.EndOfTurn,
        nonLandOnly = true,
        withAnyManaColor = true,
    ))
}

val KingNarfisBetrayal = card("King Narfi's Betrayal") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Each player mills four cards. Then you may exile a creature or planeswalker card from each graveyard.\n" +
        "II, III — Until end of turn, you may cast spells from among cards exiled with this Saga, and you " +
        "may spend mana as though it were mana of any color to cast those spells."

    sagaChapter(1) {
        effect = Effects.Pipeline {
            mill(4, Player.Each)
            val (picked) = forEachPlayerCollecting(Player.Each) {
                val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, creatureOrPlaneswalker))
                listOf(
                    chooseUpTo(
                        1,
                        from = graveyard,
                        chooser = Chooser.SourceController,
                        prompt = "You may exile a creature or planeswalker card from this graveyard",
                        selectedLabel = "Exile",
                    )
                )
            }
            exile(picked, linkToSource = true)
        }
    }

    sagaChapter(2) {
        effect = castFromBetrayalExile
    }

    sagaChapter(3) {
        effect = castFromBetrayalExile
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c30ff1fa-6639-492a-b257-934e5818c98f.jpg?1783928194"
        ruling("2021-02-05", "The chapter I ability doesn't target any cards in graveyards. For each graveyard, you may exile a creature or planeswalker card that was just milled or one that was already there. You choose which cards are exiled.")
        ruling("2021-02-05", "The chapter II/III ability doesn't change when you can cast the exiled cards. In most cases, this means during your main phase while the stack is empty, although flash may change this.")
        ruling("2021-02-05", "If you cast a spell with the chapter II/III ability, it becomes a new object. If it returns to exile, it's no longer exiled with King Narfi's Betrayal, and you won't be able to cast it again this way.")
        ruling("2021-02-05", "The \"cards exiled with King Narfi's Betrayal\" are only the cards exiled by that particular King Narfi's Betrayal, not any other Betrayals that you previously controlled.")
    }
}
