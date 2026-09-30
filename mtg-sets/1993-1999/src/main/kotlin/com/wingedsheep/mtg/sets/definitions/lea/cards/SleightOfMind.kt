package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.TextWordCategory
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

val SleightOfMind = card("Sleight of Mind") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Change the text of target spell or permanent by replacing all instances of one color word with another. (For example, you may change \"target black spell\" to \"target blue spell.\" This effect lasts indefinitely.)"

    spell {
        val subject = target(TargetSpellOrPermanent())
        effect = Effects.ChangeWordInText(setOf(TextWordCategory.COLOR_WORD), subject, Duration.Permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d427790c-e322-446e-8d7d-a6b48ad41a42.jpg?1783948701"
        ruling("2004-10-04", "You can’t Sleight proper nouns (i.e. card names). This means that you can’t affect a “Black Vise”.")
        ruling("2004-10-04", "Can target a card with no color words on it.")
        ruling("2004-10-04", "Can’t change a color word to the same color word. It must be a different word.")
        ruling("2004-10-04", "Alters all occurrences of the chosen word in the text box and the type line of the given card.")
        ruling("2004-10-04", "Can target a card with no appropriate words on it, or even one with no words at all.")
        ruling("2004-10-04", "It can’t change a word to the same word. It must be a different word.")
        ruling("2004-10-04", "It only changes what is printed on the card (or set on a token when it was created or set by a copy effect). It will not change any effects that are on the permanent.")
        ruling("2004-10-04", "You choose the words to change on resolution.")
        ruling("2004-10-04", "Changing the text of a spell will not allow you to change the targets of the spell because the targets were chosen when the spell was cast. The text change will (probably) cause it to be countered since the targets will be illegal.")
        ruling("2004-10-04", "If you change the text of a spell which is to become a permanent, the permanent will retain the text change until the effect wears off.")
    }
}
