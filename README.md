# UkeNotes

UkeNotes is an application that records audio, while the user plays a ukulele *(specifically notes - chords not yet supported)* and displays the current note being played **and** displays all notes played after the user stops recording. 

### How UkeNotes Works
##### Live Audio processing
While the audio stream is being recorded, the data is read in and filtered (with a bandpass filter) to focus on the frequency range of ukulele notes. The time series data is then transformed to the frequency domain with a fast fourier transform.  Once in the frequency domain, the fundamental frequency is extracted and displayed to the user and also stored until end of recording.

##### After Live Audio
Once the user has completed recording, the time series audio data is analyzed to find spikes that have a steep left side of the peak -- this indicates that a ukulele string was plucked.  The time of these events is matched to the previsouly tracked notes, and a list of notes played by the user is displayed.

### Original Data Collection and Analysis
