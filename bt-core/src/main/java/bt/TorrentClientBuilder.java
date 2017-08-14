package bt;

import bt.data.Storage;
import bt.magnet.MagnetUri;
import bt.magnet.MagnetUriParser;
import bt.metainfo.IMetadataService;
import bt.metainfo.Torrent;
import bt.processor.ProcessingContext;
import bt.processor.magnet.MagnetContext;
import bt.processor.torrent.TorrentContext;
import bt.runtime.BtRuntime;
import bt.torrent.PieceSelectionStrategy;
import bt.torrent.selector.PieceSelector;
import bt.torrent.selector.RarestFirstSelector;
import bt.torrent.selector.SelectorAdapter;
import bt.torrent.selector.SequentialSelector;

import java.net.URL;
import java.util.Objects;
import java.util.function.Supplier;

class TorrentClientBuilder<B extends TorrentClientBuilder> extends BaseClientBuilder<B> {

    private Storage storage;

    private URL torrentUrl;
    private Supplier<Torrent> torrentSupplier;
    private MagnetUri magnetUri;

    private PieceSelector pieceSelector;

    /**
     * @since 1.4
     */
    protected TorrentClientBuilder() {
        // set default piece selector
        this.pieceSelector = RarestFirstSelector.randomizedRarest();
    }

    /**
     * Set the provided storage as the data back-end
     *
     * @since 1.4
     */
    @SuppressWarnings("unchecked")
    public B storage(Storage storage) {
        this.storage = Objects.requireNonNull(storage, "Missing data storage");
        return (B) this;
    }

    /**
     * Set torrent file URL
     *
     * @see #torrent(Supplier)
     * @since 1.4
     */
    @SuppressWarnings("unchecked")
    public B torrent(URL torrentUrl) {
        Objects.requireNonNull(torrentUrl, "Missing torrent file URL");
        this.torrentUrl = torrentUrl;
        this.torrentSupplier = null;
        this.magnetUri = null;
        return (B) this;
    }

    /**
     * Set custom torrent file supplier
     *
     * @see #torrent(URL)
     * @since 1.4
     */
    @SuppressWarnings("unchecked")
    public B torrent(Supplier<Torrent> torrentSupplier) {
        this.torrentUrl = null;
        this.torrentSupplier = Objects.requireNonNull(torrentSupplier, "Missing torrent supplier");
        this.magnetUri = null;
        return (B) this;
    }

    /**
     * Set magnet URI in BEP-9 format
     *
     * @param magnetUri Magnet URI
     * @see MagnetUriParser
     * @since 1.4
     */
    @SuppressWarnings("unchecked")
    public B magnet(String magnetUri) {
        this.torrentUrl = null;
        this.torrentSupplier = null;
        this.magnetUri = MagnetUriParser.lenientParser().parse(magnetUri);
        return (B) this;
    }

    /**
     * Set magnet URI
     *
     * @param magnetUri Magnet URI
     * @see MagnetUriParser
     * @since 1.4
     */
    @SuppressWarnings("unchecked")
    public B magnet(MagnetUri magnetUri) {
        this.torrentUrl = null;
        this.torrentSupplier = null;
        this.magnetUri = Objects.requireNonNull(magnetUri, "Missing magnet URI");
        return (B) this;
    }

    /**
     * Set piece selection strategy
     *
     * @see #selector(PieceSelector)
     * @since 1.4
     */
    @SuppressWarnings("unchecked")
    public B selector(PieceSelectionStrategy pieceSelectionStrategy) {
        Objects.requireNonNull(pieceSelectionStrategy, "Missing piece selection strategy");
        this.pieceSelector = new SelectorAdapter(pieceSelectionStrategy);
        return (B) this;
    }

    /**
     * Set piece selection strategy
     *
     * @since 1.4
     */
    @SuppressWarnings("unchecked")
    public B selector(PieceSelector pieceSelector) {
        this.pieceSelector = Objects.requireNonNull(pieceSelector, "Missing piece selector");
        return (B) this;
    }

    /**
     * Use sequential piece selection strategy
     *
     * @since 1.4
     */
    public B sequentialSelector() {
       return selector(SequentialSelector.sequential());
    }

    /**
     * Use rarest first piece selection strategy
     *
     * @since 1.4
     */
    public B rarestSelector() {
       return selector(RarestFirstSelector.rarest());
    }

    /**
     * Use rarest first piece selection strategy
     *
     * @since 1.4
     */
    public B randomizedRarestSelector() {
       return selector(RarestFirstSelector.randomizedRarest());
    }

    @Override
    protected ProcessingContext buildProcessingContext(BtRuntime runtime) {
        Objects.requireNonNull(storage, "Missing data storage");

        ProcessingContext context;
        if (torrentUrl != null) {
            context = new TorrentContext(pieceSelector, storage, () -> fetchTorrentFromUrl(runtime, torrentUrl));
        } else if (torrentSupplier != null) {
            context = new TorrentContext(pieceSelector, storage, torrentSupplier);
        } else if (this.magnetUri != null) {
            context = new MagnetContext(magnetUri, pieceSelector, storage);
        } else {
            throw new IllegalStateException("Missing torrent supplier, torrent URL or magnet URI");
        }

        return context;
    }

    private Torrent fetchTorrentFromUrl(BtRuntime runtime, URL metainfoUrl) {
        return runtime.service(IMetadataService.class).fromUrl(metainfoUrl);
    }
}
