package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vulcan Mind-Meld
 * {U}
 * Sorcery
 *
 * Target opponent looks at the top card of your library, then exiles it face down. You look at
 * the top two cards of their library, then exile them face down. For each card exiled this way,
 * the player who exiled it may play it for as long as it remains exiled. Mana of any type can be
 * spent to cast spells this way.
 *
 * Two halves of the Gonti, Night Minister pipeline, one per direction:
 *  - your top card goes face down ([FaceDownMode.HIDDEN]) into your own exile, and the play
 *    permission goes to the target opponent — the player who exiled it. The gather uses
 *    [LookAudience.None] so the caster isn't shown the card: the opponent is the one who looks, and
 *    their may-play permission is what lets them see under a face-down exiled card.
 *  - the target opponent's top two go face down into their exile, with the permission to you. The
 *    default look audience shows them to you, which is "you look at the top two cards".
 *
 * "For as long as it remains exiled" is [MayPlayExpiry.Permanent]; the last sentence is
 * `withAnyManaType` on both grants.
 */
val VulcanMindMeld = card("Vulcan Mind-Meld") {
    manaCost = "{U}"
    typeLine = "Sorcery"
    oracleText = "Target opponent looks at the top card of your library, then exiles it face down. You look " +
        "at the top two cards of their library, then exile them face down. For each card exiled this way, " +
        "the player who exiled it may play it for as long as it remains exiled. Mana of any type can be " +
        "spent to cast spells this way."

    spell {
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val yourTop = gather(
                CardSource.TopOfLibrary(count = 1, player = Player.You),
                lookAudience = LookAudience.None,
            )
            exile(yourTop, Player.You, faceDown = FaceDownMode.HIDDEN)
            run(
                Effects.GrantMayPlayFromExile(
                    from = yourTop,
                    expiry = MayPlayExpiry.Permanent,
                    withAnyManaType = true,
                    recipient = EffectTarget.PlayerRef(Player.TargetOpponent),
                )
            )
            val theirTop = gather(CardSource.TopOfLibrary(count = 2, player = Player.TargetOpponent))
            exile(theirTop, Player.TargetOpponent, faceDown = FaceDownMode.HIDDEN)
            run(
                Effects.GrantMayPlayFromExile(
                    from = theirTop,
                    expiry = MayPlayExpiry.Permanent,
                    withAnyManaType = true,
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Pavel Kolomeyets"
        flavorText = "\"My mind to your mind. My thoughts to your thoughts.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd6c0c12-01b8-4f68-9401-7430a7f94bf7.jpg?1793274240"
    }
}
