package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tajuru Paragon — Zendikar Rising #209
 * {1}{G} · Creature — Elf · 3/2
 *
 * Tajuru Paragon is also a Cleric, Rogue, Warrior, and Wizard.
 * Kicker {3}
 * When this creature enters, if it was kicked, reveal the top six cards of your library. You may
 * put a card that shares a creature type with it from among them into your hand. Put the rest on
 * the bottom of your library in a random order.
 *
 * The four self-[GrantSubtype]s are a characteristic-defining ability (CR 604.3): the card is an
 * Elf Cleric Rogue Warrior Wizard in every zone, so it fills a whole party and counts as each type
 * in hand, on the stack and in the graveyard too. "Shares a creature type with it" reads the
 * Paragon's last-known types if it has left the battlefield before the trigger resolves.
 */
val TajuruParagon = card("Tajuru Paragon") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf"
    oracleText = "Tajuru Paragon is also a Cleric, Rogue, Warrior, and Wizard.\n" +
        "Kicker {3} (You may pay an additional {3} as you cast this spell.)\n" +
        "When this creature enters, if it was kicked, reveal the top six cards of your library. You may " +
        "put a card that shares a creature type with it from among them into your hand. Put the rest on " +
        "the bottom of your library in a random order."
    power = 3
    toughness = 2

    for (subtype in listOf("Cleric", "Rogue", "Warrior", "Wizard")) {
        staticAbility { ability = GrantSubtype(subtype) }
    }

    keywordAbility(KeywordAbility.kicker("{3}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(6), revealed = true)
            val (kept, rest) = chooseUpToSplit(
                1,
                from = revealed,
                filter = GameObjectFilter.Any.sharingCreatureTypeWith(EffectTarget.Self),
                prompt = "You may put a card that shares a creature type with Tajuru Paragon into your hand.",
                showAllCards = true
            )
            toHand(kept)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a045dd0f-953a-4022-8f29-0758f2b6ccaa.jpg"
        ruling(
            "2020-09-25",
            "If Tajuru Paragon leaves the battlefield before its triggered ability resolves, use its " +
                "creature types as it last existed on the battlefield to determine which cards you may put " +
                "into your hand."
        )
        ruling(
            "2020-09-25",
            "The ability that adds creature types to Tajuru Paragon applies in all zones, not just the battlefield."
        )
    }
}
